<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.12.1" name="objects" tilewidth="96" tileheight="128" tilecount="58" columns="0">
 <grid orientation="orthogonal" width="1" height="1"/>
 <tile id="1" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="animationSpeed" type="float" value="1"/>
   <property name="attackSound" value="SWING"/>
   <property name="damage" type="float" value="7"/>
   <property name="damageDelay" type="float" value="0.2"/>
   <property name="life" type="int" value="100"/>
   <property name="lifeReg" type="float" value="0.25"/>
   <property name="speed" type="float" value="3.5"/>
  </properties>
  <image source="objects/player.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="11" y="18" width="9" height="5">
    <ellipse/>
   </object>
   <object id="2" name="attack_sensor_down" x="0" y="17" width="32" height="15">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="3" name="attack_sensor_up" x="0" y="0" width="32" height="15">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="4" name="attack_sensor_left" x="0" y="0" width="15" height="32">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
   <object id="5" name="attack_sensor_right" x="17" y="0" width="15" height="32">
    <properties>
     <property name="sensor" type="bool" value="true"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="2" type="Prop">
  <image source="objects/house.png" width="80" height="112"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="9.625" y="66.5" width="60.875" height="40"/>
  </objectgroup>
 </tile>
 <tile id="29" type="Prop">
  <image source="objects/house_back.png" width="80" height="112"/>
 </tile>
 <tile id="8" type="Prop">
  <image source="objects/House_1_Wood_Base_Blue.png" width="96" height="128"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="15.4545" y="72.5455" width="65.4545" height="41.8182"/>
  </objectgroup>
 </tile>
 <tile id="30" type="Prop">
  <image source="objects/House_1_Wood_Base_Blue_Back.png" width="96" height="128"/>
 </tile>
 <tile id="4" type="Prop">
  <image source="objects/chest.png" width="16" height="16"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="4" width="16" height="10"/>
  </objectgroup>
 </tile>
 <tile id="5" type="Prop">
  <image source="objects/oak_tree.png" width="41" height="63"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="13" y="54.1696">
    <polygon points="0,0 6,0.830357 11,0.830357 16,-0.830357 16,-1.66071 14,-4.15179 13,-10.7946 3,-10.7946 3,-4.98214 2,-4.15179 1,-2.49107 0,-0.830357"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="18" type="Prop">
  <image source="objects/small_tree.png" width="32" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="13.8889" y="30.3778" width="4.22222" height="2.11111"/>
  </objectgroup>
 </tile>
 <tile id="17" type="Object">
  <properties>
   <property name="bodyType" propertytype="BodyType" value="StaticBody"/>
  </properties>
  <image source="objects/small_tree_cut.png" width="32" height="32"/>
 </tile>
 <tile id="16" type="Prop">
  <image source="objects/tree_trunk.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="13.9778" y="14.4" width="4.02222" height="2.06667"/>
  </objectgroup>
 </tile>
 <tile id="6" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="z" type="int" value="0"/>
  </properties>
  <image source="objects/trap.png" width="16" height="16"/>
 </tile>
 <tile id="7" type="Object">
  <properties>
   <property name="animation" value="IDLE"/>
   <property name="animationSpeed" type="float" value="1"/>
   <property name="bodyType" propertytype="BodyType" value="StaticBody"/>
   <property name="life" type="int" value="99999"/>
   <property name="lifeReg" type="float" value="9999"/>
  </properties>
  <image source="objects/training_dummy.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="11.0625" y="20.9565" width="9.875" height="7.04348"/>
  </objectgroup>
 </tile>
 <tile id="9" type="Prop">
  <image source="objects/lantern.png" width="16" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="4.95503" y="40.9416" width="7.05717" height="5.05513"/>
  </objectgroup>
 </tile>
 <tile id="10" type="Prop">
  <image source="objects/fallen_trunk.png" width="32" height="16"/>
 </tile>
 <tile id="11" type="Prop">
  <image source="objects/bridge_0.png" width="48" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="9" y="30.8125" width="30.0625" height="2.0625"/>
   <object id="2" x="8.78125" y="13.9688" width="30.0625" height="2.0625"/>
   <object id="3" x="10.1875" y="32.75">
    <polygon points="0,0 2.8125,2.9375 5.75,-0.0625"/>
   </object>
   <object id="4" x="32.25" y="32.875">
    <polygon points="0,0 2.8125,2.9375 5.75,-0.0625"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="12" type="Prop">
  <image source="objects/bridge_2.png" width="48" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="14" y="9" width="2" height="30"/>
   <object id="2" x="31.875" y="9" width="2" height="30"/>
   <object id="3" x="10.9375" y="11.875" width="4.0625" height="4"/>
   <object id="4" x="32.9688" y="11.9375" width="4.0625" height="4"/>
   <object id="5" x="10.7813" y="36.25" width="4.0625" height="4"/>
   <object id="6" x="33.0313" y="36.1875" width="4.0625" height="4"/>
  </objectgroup>
 </tile>
 <tile id="13" type="Prop">
  <image source="objects/bridge_1.png" width="48" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="14.0624" y="9.0625" width="2" height="30"/>
   <object id="2" x="31.9374" y="9.0625" width="2" height="30"/>
   <object id="3" x="10.9999" y="11.9375" width="4.0625" height="4"/>
   <object id="4" x="33.0313" y="12" width="4.0625" height="4"/>
   <object id="5" x="10.8437" y="36.3125" width="4.0625" height="4"/>
   <object id="6" x="33.0938" y="36.25" width="4.0625" height="4"/>
  </objectgroup>
 </tile>
 <tile id="20" type="Prop">
  <image source="objects/bridge_4.png" width="48" height="48"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="9.07813" y="30.9361" width="30.0625" height="2.0625"/>
   <object id="2" x="8.85938" y="14.0924" width="30.0625" height="2.0625"/>
   <object id="3" x="10.2656" y="32.8736">
    <polygon points="0,0 2.8125,2.9375 5.75,-0.0625"/>
   </object>
   <object id="4" x="32.3281" y="32.9986">
    <polygon points="0,0 2.8125,2.9375 5.75,-0.0625"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="21" type="Creature">
  <properties>
   <property name="damage" type="float" value="7"/>
   <property name="damageDelay" type="float" value="7"/>
   <property name="speed" type="float" value="2"/>
  </properties>
  <image source="../raw/creatures/cow/cow_idle_left_0.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.875" y="20.8125" width="17.1875" height="5.125"/>
  </objectgroup>
  <animation>
   <frame tileid="21" duration="500"/>
   <frame tileid="22" duration="500"/>
  </animation>
 </tile>
 <tile id="22">
  <image source="../raw/creatures/cow/cow_idle_left_1.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.90625" y="20.9375" width="17.1875" height="5.125"/>
  </objectgroup>
 </tile>
 <tile id="23" type="Creature">
  <properties>
   <property name="creature_type" propertytype="CreatureType" value="pig"/>
   <property name="damage" type="float" value="4"/>
   <property name="damageDelay" type="float" value="5"/>
   <property name="life" type="float" value="90"/>
   <property name="speed" type="float" value="2"/>
  </properties>
  <image source="../raw/creatures/pig/pig_idle_left_0.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.71875" y="18.8125" width="14.25" height="4.1875"/>
  </objectgroup>
  <animation>
   <frame tileid="23" duration="500"/>
   <frame tileid="24" duration="500"/>
  </animation>
 </tile>
 <tile id="24">
  <image source="../raw/creatures/pig/pig_idle_left_1.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.75" y="18.7813" width="14.25" height="4.1875"/>
  </objectgroup>
 </tile>
 <tile id="25" type="Creature">
  <properties>
   <property name="creature_type" propertytype="CreatureType" value="sheep"/>
   <property name="damage" type="float" value="4"/>
   <property name="damageDelay" type="float" value="5"/>
   <property name="life" type="float" value="90"/>
   <property name="speed" type="float" value="2"/>
  </properties>
  <image source="../raw/creatures/sheep/sheep_idle_left_0.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.9375" y="19.3125" width="14.1563" height="3.6875"/>
  </objectgroup>
  <animation>
   <frame tileid="25" duration="500"/>
   <frame tileid="26" duration="500"/>
  </animation>
 </tile>
 <tile id="26">
  <image source="../raw/creatures/sheep/sheep_idle_left_1.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="8.85935" y="19.3125" width="14.1563" height="3.6875"/>
  </objectgroup>
 </tile>
 <tile id="27" type="Creature">
  <properties>
   <property name="creature_type" propertytype="CreatureType" value="chicken"/>
   <property name="damage" type="float" value="2"/>
   <property name="damageDelay" type="float" value="0.3"/>
   <property name="life" type="float" value="30"/>
   <property name="speed" type="float" value="4.5"/>
  </properties>
  <image source="../raw/creatures/chicken/chicken_idle_left_0.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="11.4375" y="20.375" width="8.96875" height="2.65625"/>
  </objectgroup>
  <animation>
   <frame tileid="27" duration="500"/>
   <frame tileid="28" duration="500"/>
  </animation>
 </tile>
 <tile id="28">
  <image source="../raw/creatures/chicken/chicken_idle_left_1.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="11.4531" y="20.3281" width="8.96875" height="2.65625"/>
  </objectgroup>
 </tile>
 <tile id="32" type="Creature">
  <properties>
   <property name="creature_type" propertytype="CreatureType" value="skeleton"/>
   <property name="damage" type="float" value="12"/>
   <property name="damageDelay" type="float" value="1"/>
   <property name="hostile" type="bool" value="true"/>
   <property name="life" type="float" value="120"/>
  </properties>
  <image source="../raw/creatures/skeleton/skeleton_idle_right_00.png" width="32" height="32"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="12.125" y="21.125" width="7.625" height="2.625"/>
  </objectgroup>
  <animation>
   <frame tileid="32" duration="250"/>
   <frame tileid="33" duration="250"/>
   <frame tileid="34" duration="250"/>
   <frame tileid="35" duration="250"/>
   <frame tileid="36" duration="250"/>
   <frame tileid="37" duration="250"/>
  </animation>
 </tile>
 <tile id="33">
  <image source="../raw/creatures/skeleton/skeleton_idle_right_01.png" width="32" height="32"/>
 </tile>
 <tile id="34">
  <image source="../raw/creatures/skeleton/skeleton_idle_right_02.png" width="32" height="32"/>
 </tile>
 <tile id="35">
  <image source="../raw/creatures/skeleton/skeleton_idle_right_03.png" width="32" height="32"/>
 </tile>
 <tile id="36">
  <image source="../raw/creatures/skeleton/skeleton_idle_right_04.png" width="32" height="32"/>
 </tile>
 <tile id="37">
  <image source="../raw/creatures/skeleton/skeleton_idle_right_05.png" width="32" height="32"/>
 </tile>
 <tile id="38" type="Creature">
  <properties>
   <property name="creature_type" propertytype="CreatureType" value="slime"/>
   <property name="damage" type="float" value="9"/>
   <property name="damageDelay" type="float" value="1.4"/>
   <property name="hostile" type="bool" value="true"/>
   <property name="life" type="float" value="200"/>
   <property name="speed" type="float" value="2"/>
  </properties>
  <image source="../raw/creatures/slime_green/slime_green_idle_00.png" width="64" height="64"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="26.25" y="34.125" width="12" height="5.375"/>
  </objectgroup>
  <animation>
   <frame tileid="38" duration="300"/>
   <frame tileid="39" duration="300"/>
   <frame tileid="40" duration="300"/>
   <frame tileid="41" duration="300"/>
  </animation>
 </tile>
 <tile id="39">
  <image source="../raw/creatures/slime_green/slime_green_idle_01.png" width="64" height="64"/>
 </tile>
 <tile id="40">
  <image source="../raw/creatures/slime_green/slime_green_idle_02.png" width="64" height="64"/>
 </tile>
 <tile id="41">
  <image source="../raw/creatures/slime_green/slime_green_idle_03.png" width="64" height="64"/>
 </tile>
 <tile id="31">
  <image source="../raw/items/potions/potion_red.png" width="8" height="8"/>
 </tile>
 <tile id="42">
  <image source="../raw/items/potions/potion_blue.png" width="8" height="8"/>
 </tile>
 <tile id="43">
  <image source="../raw/items/potions/potion_green.png" width="8" height="8"/>
 </tile>
 <tile id="44">
  <image source="../raw/items/potions/potion_purple.png" width="8" height="8"/>
 </tile>
 <tile id="46">
  <image source="../raw/items/potions/potion_yellow.png" width="8" height="8"/>
 </tile>
 <tile id="47">
  <image source="../raw/items/misc/shard_blue.png" width="8" height="8"/>
 </tile>
 <tile id="52">
  <image source="../raw/items/coins/coin_gold.png" width="8" height="8"/>
 </tile>
 <tile id="53">
  <image source="../raw/items/coins/coin_silver.png" width="8" height="8"/>
 </tile>
 <tile id="54">
  <image source="../raw/items/coins/coin_bronze.png" width="8" height="8"/>
 </tile>
 <tile id="55">
  <image source="../raw/items/food/chicken_cooked.png" width="16" height="16"/>
 </tile>
 <tile id="56">
  <image source="../raw/items/food/chicken_raw.png" width="16" height="16"/>
 </tile>
 <tile id="57">
  <image source="../raw/items/food/steak_cooked.png" width="16" height="16"/>
 </tile>
 <tile id="58">
  <image source="../raw/items/food/steak_raw.png" width="16" height="16"/>
 </tile>
 <tile id="59">
  <image source="../raw/items/tools/axe.png" width="16" height="16"/>
 </tile>
 <tile id="60">
  <image source="../raw/items/tools/hoe.png" width="16" height="16"/>
 </tile>
 <tile id="61">
  <image source="../raw/items/tools/pickaxe.png" width="16" height="16"/>
 </tile>
 <tile id="69">
  <image source="../raw/items/tools/sword.png" width="16" height="16"/>
 </tile>
 <tile id="62">
  <image source="../raw/items/tools/watering_can.png" width="16" height="16"/>
 </tile>
 <tile id="63">
  <image source="../raw/objects/misc/campfire_0.png" width="16" height="16"/>
  <animation>
   <frame tileid="63" duration="250"/>
   <frame tileid="64" duration="250"/>
   <frame tileid="65" duration="250"/>
  </animation>
 </tile>
 <tile id="64">
  <image source="../raw/objects/misc/campfire_1.png" width="16" height="16"/>
 </tile>
 <tile id="65">
  <image source="../raw/objects/misc/campfire_2.png" width="16" height="16"/>
 </tile>
 <tile id="70">
  <image source="../raw/objects/misc/wet_dirt.png" width="16" height="16"/>
 </tile>
</tileset>
