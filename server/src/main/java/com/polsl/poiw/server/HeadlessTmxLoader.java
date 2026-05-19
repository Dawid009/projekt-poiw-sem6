package com.polsl.poiw.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.Array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/**
 * Headless TMX loader — parsuje XML mapy Tiled bez ładowania tekstur.
 * Server nie potrzebuje grafiki, tylko collision shapes, spawn points, triggers.
 */
public class HeadlessTmxLoader {

    private static final String TAG = "HeadlessTmxLoader";
    private static final long FLAG_FLIP_HORIZONTALLY = 0x80000000L;
    private static final long FLAG_FLIP_VERTICALLY = 0x40000000L;
    private static final long FLAG_FLIP_DIAGONALLY = 0x20000000L;
    private static final long MASK_CLEAR_TILE_FLAGS = 0x1FFFFFFFL;

    /**
     * per-tile data parsed from TSX — collision shapes, tile type, image dimensions.
     * keyed by global tile ID (firstGid + localTileId).
     */
    public record TileData(String type, float imageW, float imageH,
                           float collX, float collY, float collW, float collH,
                           boolean hasCollision, Map<String, Object> properties) {}

    private final Map<Integer, TileData> tileDataMap = new HashMap<>();

    /** returns parsed tile data by global ID (for ServerTiledObjectFactory) */
    public TileData getTileData(int gid) {
        return tileDataMap.get(gid);
    }

    /**
     * Loads a TiledMap from a .tmx file without loading any textures.
     * Only parses: map dimensions, tile layers (gid data), object layers.
     */
    public TiledMap load(String filePath) {
        FileHandle file = Gdx.files.internal(filePath);
        if (!file.exists()) {
            throw new com.badlogic.gdx.utils.GdxRuntimeException("TMX file not found: " + filePath);
        }

        XmlReader reader = new XmlReader();
        XmlReader.Element root = reader.parse(file);

        TiledMap map = new TiledMap();
        MapProperties mapProps = map.getProperties();

        int mapWidth = root.getIntAttribute("width", 0);
        int mapHeight = root.getIntAttribute("height", 0);
        int tileWidth = root.getIntAttribute("tilewidth", 0);
        int tileHeight = root.getIntAttribute("tileheight", 0);

        mapProps.put("width", mapWidth);
        mapProps.put("height", mapHeight);
        mapProps.put("tilewidth", tileWidth);
        mapProps.put("tileheight", tileHeight);

        // parse tilesets (metadata only, no images)
        parseTilesets(root, map, file);

        // parse layers
        for (int i = 0; i < root.getChildCount(); i++) {
            XmlReader.Element child = root.getChild(i);
            String name = child.getName();

            if ("layer".equals(name)) {
                parseTileLayer(child, map, mapWidth, mapHeight, tileWidth, tileHeight);
            } else if ("objectgroup".equals(name)) {
                parseObjectGroup(child, map, mapHeight * tileHeight);
            }
        }

        Gdx.app.log(TAG, "Loaded headless TMX: " + filePath
            + " (" + mapWidth + "x" + mapHeight + " tiles, " + map.getLayers().getCount() + " layers)");
        return map;
    }

    private void parseTilesets(XmlReader.Element root, TiledMap map, FileHandle tmxFile) {
        Array<XmlReader.Element> tilesetElements = root.getChildrenByName("tileset");
        for (XmlReader.Element tsElement : tilesetElements) {
            int firstGid = tsElement.getIntAttribute("firstgid", 1);

            // external TSX?
            String source = tsElement.getAttribute("source", null);
            XmlReader.Element tsRoot = tsElement;
            if (source != null) {
                FileHandle tsxFile = tmxFile.parent().child(source);
                if (tsxFile.exists()) {
                    XmlReader tsxReader = new XmlReader();
                    tsRoot = tsxReader.parse(tsxFile);
                }
            }

            String tsName = tsRoot.getAttribute("name", "tileset");
            int tileCount = tsRoot.getIntAttribute("tilecount", 0);

            TiledMapTileSet tileSet = new TiledMapTileSet();
            tileSet.setName(tsName);
            tileSet.getProperties().put("firstgid", firstGid);
            tileSet.getProperties().put("tilecount", tileCount);

            // parse per-tile data (collision shapes, type, image size)
            Array<XmlReader.Element> tileElements = tsRoot.getChildrenByName("tile");
            for (XmlReader.Element tileEl : tileElements) {
                int tileId = tileEl.getIntAttribute("id", 0);
                int globalId = firstGid + tileId;

                String tileType = tileEl.getAttribute("type", "");
                Map<String, Object> tileProperties = new HashMap<>();
                XmlReader.Element tilePropsEl = tileEl.getChildByName("properties");
                if (tilePropsEl != null) {
                    parsePropertiesElement(tilePropsEl, tileProperties);
                }

                // image dimensions
                float imgW = 32f, imgH = 32f;
                XmlReader.Element imageEl = tileEl.getChildByName("image");
                if (imageEl != null) {
                    imgW = imageEl.getFloatAttribute("width", 32f);
                    imgH = imageEl.getFloatAttribute("height", 32f);
                }

                // collision shape from objectgroup (first non-sensor object)
                float collX = 0, collY = 0, collW = 0, collH = 0;
                boolean hasCollision = false;
                XmlReader.Element objGroup = tileEl.getChildByName("objectgroup");
                if (objGroup != null) {
                    for (int j = 0; j < objGroup.getChildCount(); j++) {
                        XmlReader.Element objEl = objGroup.getChild(j);
                        if (!"object".equals(objEl.getName())) continue;

                        // skip sensors
                        XmlReader.Element propsEl = objEl.getChildByName("properties");
                        if (propsEl != null) {
                            boolean isSensor = false;
                            for (XmlReader.Element p : propsEl.getChildrenByName("property")) {
                                if ("sensor".equals(p.getAttribute("name", ""))
                                    && "true".equals(p.getAttribute("value", ""))) {
                                    isSensor = true;
                                    break;
                                }
                            }
                            if (isSensor) continue;
                        }

                        // use bounding box (works for rect, ellipse, polygon)
                        collX = objEl.getFloatAttribute("x", 0);
                        collY = objEl.getFloatAttribute("y", 0);
                        collW = objEl.getFloatAttribute("width", 0);
                        collH = objEl.getFloatAttribute("height", 0);

                        // polygon → compute bounding box
                        XmlReader.Element polyEl = objEl.getChildByName("polygon");
                        if (polyEl != null && collW == 0 && collH == 0) {
                            String points = polyEl.getAttribute("points", "");
                            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                            for (String pt : points.split(" ")) {
                                String[] xy = pt.split(",");
                                if (xy.length < 2) continue;
                                float px = Float.parseFloat(xy[0]);
                                float py = Float.parseFloat(xy[1]);
                                minX = Math.min(minX, px);
                                minY = Math.min(minY, py);
                                maxX = Math.max(maxX, px);
                                maxY = Math.max(maxY, py);
                            }
                            collX += minX;
                            collY += minY;
                            collW = maxX - minX;
                            collH = maxY - minY;
                        }

                        // ellipse → use x,y,w,h directly (already set)
                        hasCollision = collW > 0 && collH > 0;
                        if (hasCollision) break; // use first non-sensor shape
                    }
                }

                // TSX collision shapes are in Tiled Y-down space (origin=top-left of tile).
                // LibGDX client flips them to Y-up automatically. Server must do the same:
                //   collY_yup = imageH - collY_ydown - collH
                if (hasCollision) {
                    collY = imgH - collY - collH;
                }

                tileDataMap.put(globalId, new TileData(tileType, imgW, imgH,
                    collX, collY, collW, collH, hasCollision, Map.copyOf(tileProperties)));

                TiledMapTile tile = new StaticTiledMapTile(new TextureRegion());
                tile.setId(globalId);
                if (!tileType.isBlank()) {
                    tile.getProperties().put("type", tileType);
                }
                for (Map.Entry<String, Object> entry : tileProperties.entrySet()) {
                    tile.getProperties().put(entry.getKey(), entry.getValue());
                }
                if (hasCollision) {
                    tile.getObjects().add(new RectangleMapObject(collX, collY, collW, collH));
                }
                tileSet.putTile(globalId, tile);
            }

            map.getTileSets().addTileSet(tileSet);
        }
    }

    private void parseTileLayer(XmlReader.Element element, TiledMap map,
                                 int mapWidth, int mapHeight, int tileWidth, int tileHeight) {
        String layerName = element.getAttribute("name", "");
        TiledMapTileLayer layer = new TiledMapTileLayer(mapWidth, mapHeight, tileWidth, tileHeight);
        layer.setName(layerName);

        // parse properties
        parseLayerProperties(element, layer.getProperties());

        XmlReader.Element dataElement = element.getChildByName("data");
        if (dataElement != null) {
            populateTileLayerCells(dataElement, map, layer, mapWidth, mapHeight);
        }

        map.getLayers().add(layer);
    }

    private void populateTileLayerCells(XmlReader.Element dataElement,
                                        TiledMap map,
                                        TiledMapTileLayer layer,
                                        int mapWidth,
                                        int mapHeight) {
        String encoding = dataElement.getAttribute("encoding", "");
        String compression = dataElement.getAttribute("compression", "");
        if (!"base64".equals(encoding)) {
            return;
        }

        String rawData = dataElement.getText();
        if (rawData == null || rawData.isBlank()) {
            return;
        }

        byte[] decoded = Base64.getDecoder().decode(rawData.replaceAll("\\s+", ""));
        byte[] bytes = decompressLayerBytes(decoded, compression);
        int cellCount = Math.min(mapWidth * mapHeight, bytes.length / 4);

        for (int index = 0; index < cellCount; index++) {
            int byteIndex = index * 4;
            long rawGid = ((long) bytes[byteIndex] & 0xFFL)
                | (((long) bytes[byteIndex + 1] & 0xFFL) << 8)
                | (((long) bytes[byteIndex + 2] & 0xFFL) << 16)
                | (((long) bytes[byteIndex + 3] & 0xFFL) << 24);
            int gid = clearTileFlags(rawGid);
            if (gid == 0) {
                continue;
            }

            TiledMapTile tile = map.getTileSets().getTile(gid);
            if (tile == null) {
                continue;
            }

            int x = index % mapWidth;
            int y = mapHeight - 1 - (index / mapWidth);
            TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
            cell.setTile(tile);
            layer.setCell(x, y, cell);
        }
    }

    private byte[] decompressLayerBytes(byte[] data, String compression) {
        if (compression == null || compression.isBlank()) {
            return data;
        }

        try (InputStream input = switch (compression) {
            case "zlib" -> new InflaterInputStream(new ByteArrayInputStream(data));
            default -> throw new IllegalArgumentException("Unsupported TMX compression: " + compression);
        }) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decompress TMX layer data", exception);
        }
    }

    private void parseObjectGroup(XmlReader.Element element, TiledMap map, int mapHeightPx) {
        String layerName = element.getAttribute("name", "");
        MapLayer layer = new MapLayer();
        layer.setName(layerName);

        parseLayerProperties(element, layer.getProperties());

        Array<XmlReader.Element> objects = element.getChildrenByName("object");
        for (XmlReader.Element objEl : objects) {
            MapObject mapObject = parseObject(objEl, mapHeightPx);
            if (mapObject != null) {
                layer.getObjects().add(mapObject);
            }
        }

        map.getLayers().add(layer);
        Gdx.app.debug(TAG, "Object layer '" + layerName + "': " + objects.size + " objects");
    }

    private MapObject parseObject(XmlReader.Element objEl, int mapHeightPx) {
        float x = objEl.getFloatAttribute("x", 0);
        float y = objEl.getFloatAttribute("y", 0);
        float width = objEl.getFloatAttribute("width", 0);
        float height = objEl.getFloatAttribute("height", 0);
        long rawGid = parseRawGid(objEl);
        int gid = clearTileFlags(rawGid);
        String name = objEl.getAttribute("name", "");
        String type = objEl.getAttribute("type", objEl.getAttribute("class", ""));

        // Tiled uses Y-down, LibGDX uses Y-up — flip Y axis
        if (gid > 0) {
            // tile object: TMX y points to bottom of tile image, flip to get bottom-left in Y-up
            y = mapHeightPx - y;
        } else {
            // rectangle object: TMX y points to top of rect, flip and adjust
            y = mapHeightPx - y - height;
        }

        MapObject mapObject;

        if (gid > 0) {
            // tile object — create TiledMapTileMapObject (without actual tile reference)
            // the server uses gid to identify object type from tsx
            RectangleMapObject rectObj = new RectangleMapObject(x, y, width, height);
            mapObject = rectObj;
            mapObject.getProperties().put("gid", gid);
            mapObject.getProperties().put("rawGid", rawGid);
            mapObject.getProperties().put("flipHorizontally", hasTileFlag(rawGid, FLAG_FLIP_HORIZONTALLY));
            mapObject.getProperties().put("flipVertically", hasTileFlag(rawGid, FLAG_FLIP_VERTICALLY));
            mapObject.getProperties().put("flipDiagonally", hasTileFlag(rawGid, FLAG_FLIP_DIAGONALLY));
        } else {
            // rectangle object
            RectangleMapObject rectObj = new RectangleMapObject(x, y, width, height);
            mapObject = rectObj;
        }

        mapObject.setName(name);
        mapObject.getProperties().put("type", type);
        mapObject.getProperties().put("x", x);
        mapObject.getProperties().put("y", y);
        mapObject.getProperties().put("width", width);
        mapObject.getProperties().put("height", height);

        // parse custom properties
        XmlReader.Element propsEl = objEl.getChildByName("properties");
        if (propsEl != null) {
            for (XmlReader.Element propEl : propsEl.getChildrenByName("property")) {
                String propName = propEl.getAttribute("name", "");
                String propType = propEl.getAttribute("type", "string");
                String propValue = propEl.getAttribute("value", propEl.getText() != null ? propEl.getText() : "");

                switch (propType) {
                    case "int" -> mapObject.getProperties().put(propName, Integer.parseInt(propValue));
                    case "float" -> mapObject.getProperties().put(propName, Float.parseFloat(propValue));
                    case "bool" -> mapObject.getProperties().put(propName, Boolean.parseBoolean(propValue));
                    default -> mapObject.getProperties().put(propName, propValue);
                }
            }
        }

        return mapObject;
    }

    private long parseRawGid(XmlReader.Element objEl) {
        String gidValue = objEl.getAttribute("gid", null);
        if (gidValue == null || gidValue.isBlank()) {
            return 0L;
        }

        return Long.parseLong(gidValue);
    }

    private int clearTileFlags(long rawGid) {
        return (int) (rawGid & MASK_CLEAR_TILE_FLAGS);
    }

    private boolean hasTileFlag(long rawGid, long flag) {
        return (rawGid & flag) != 0L;
    }

    private void parseLayerProperties(XmlReader.Element element, MapProperties props) {
        XmlReader.Element propsEl = element.getChildByName("properties");
        if (propsEl == null) return;

        parsePropertiesElement(propsEl, props);
    }

    private void parsePropertiesElement(XmlReader.Element propsEl, MapProperties target) {
        for (XmlReader.Element propEl : propsEl.getChildrenByName("property")) {
            String name = propEl.getAttribute("name", "");
            String type = propEl.getAttribute("type", "string");
            String value = propEl.getAttribute("value", propEl.getText() != null ? propEl.getText() : "");

            switch (type) {
                case "int" -> target.put(name, Integer.parseInt(value));
                case "float" -> target.put(name, Float.parseFloat(value));
                case "bool" -> target.put(name, Boolean.parseBoolean(value));
                default -> target.put(name, value);
            }
        }
    }

    private void parsePropertiesElement(XmlReader.Element propsEl, Map<String, Object> target) {
        for (XmlReader.Element propEl : propsEl.getChildrenByName("property")) {
            String name = propEl.getAttribute("name", "");
            String type = propEl.getAttribute("type", "string");
            String value = propEl.getAttribute("value", propEl.getText() != null ? propEl.getText() : "");

            switch (type) {
                case "int" -> target.put(name, Integer.parseInt(value));
                case "float" -> target.put(name, Float.parseFloat(value));
                case "bool" -> target.put(name, Boolean.parseBoolean(value));
                default -> target.put(name, value);
            }
        }
    }
}
