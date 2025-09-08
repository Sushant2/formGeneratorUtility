INSERT INTO FIM_BUILDER_MASTER_DATA (
    FIELD_ID,
    FIELD_NAME,
    OPTION_ID,
    OPTION_VALUE,
    TABLE_ANCHOR,
    IS_ACTIVE,
    DEPENDENT_VALUE,
    ORDER_NO
) VALUES
(0, '_weekDays1642364703', 1, 'Monday', 'storehoursnd21214306162', 'Y', NULL, 1),
(0, '_weekDays1642364703', 2, 'Tuesday', 'storehoursnd21214306162', 'Y', NULL, 2),
(0, '_weekDays1642364703', 3, 'Wednesday', 'storehoursnd21214306162', 'Y', NULL, 3),
(0, '_weekDays1642364703', 4, 'Thrusday', 'storehoursnd21214306162', 'Y', NULL, 4),
(0, '_weekDays1642364703', 5, 'Friday', 'storehoursnd21214306162', 'Y', NULL, 5),
(0, '_weekDays1642364703', 6, 'Saturday', 'storehoursnd21214306162', 'Y', NULL, 6),
(0, '_weekDays1642364703', 7, 'Sunday', 'storehoursnd21214306162', 'Y', NULL, 7),
(0, '_closeAllDaymonday1541029386', 1, 'Yes', 'storehoursnd21214306162', 'Y', NULL, 1),
(0, '_closeAllDaymonday1541029386', 2, 'No', 'storehoursnd21214306162', 'Y', NULL, 2);


DELETE FROM FIM_BUILDER_MASTER_DATA WHERE FIELD_NAME IN ('_morningFromHours1480792847','_morningToHours1107361967','_afternoonFromHours2042517178','_afternoonToHours1748811626');

SET @A:=0;
INSERT INTO FIM_BUILDER_MASTER_DATA(FIELD_ID,FIELD_NAME,OPTION_ID,OPTION_VALUE,TABLE_ANCHOR,IS_ACTIVE,DEPENDENT_VALUE,ORDER_NO) 
SELECT '0','_morningFromHours1480792847',@A:=@A+1,Morning_From_Hours,'storetimings887582445','Y',NULL,@A:=@A+1 FROM Sheet1;

SET @A:=0;
INSERT INTO FIM_BUILDER_MASTER_DATA(FIELD_ID,FIELD_NAME,OPTION_ID,OPTION_VALUE,TABLE_ANCHOR,IS_ACTIVE,DEPENDENT_VALUE,ORDER_NO) 
SELECT '0','_morningToHours1107361967',@A:=@A+1,Morning_To_Hours,'storetimings887582445','Y',NULL,@A:=@A+1 FROM Sheet1;

SET @A:=0;
INSERT INTO FIM_BUILDER_MASTER_DATA(FIELD_ID,FIELD_NAME,OPTION_ID,OPTION_VALUE,TABLE_ANCHOR,IS_ACTIVE,DEPENDENT_VALUE,ORDER_NO) 
SELECT '0','_afternoonFromHours2042517178',@A:=@A+1,Afternoon_From_Hours,'storetimings887582445','Y',NULL,@A:=@A+1 FROM Sheet1;

SET @A:=0;
INSERT INTO FIM_BUILDER_MASTER_DATA(FIELD_ID,FIELD_NAME,OPTION_ID,OPTION_VALUE,TABLE_ANCHOR,IS_ACTIVE,DEPENDENT_VALUE,ORDER_NO) 
SELECT '0','_afternoonToHours1748811626',@A:=@A+1,Afternoon_To_Hours,'storetimings887582445','Y',NULL,@A:=@A+1 FROM Sheet1;

UPDATE FIM_BUILDER_MASTER_DATA SET OPTION_VALUE = DATE_FORMAT(STR_TO_DATE(OPTION_VALUE, '%d/%m/%Y %r'), '%H:%i') WHERE FIELD_NAME IN ('_morningFromHours1480792847','_morningToHours1107361967','_afternoonFromHours2042517178','_afternoonToHours1748811626');
SELECT * FROM FIM_BUILDER_MASTER_DATA WHERE FIELD_NAME IN ('_morningFromHours1480792847','_morningToHours1107361967','_afternoonFromHours2042517178','_afternoonToHours1748811626');

UPDATE FIM_BUILDER_MASTER_DATA SET DEPENDENT_VALUE=2 WHERE FIELD_NAME IN ('_morningFromHours1480792847','_morningToHours1107361967','_afternoonFromHours2042517178','_afternoonToHours1748811626');

INSERT INTO CLIENT_XMLS (
    NAME,
    XML_KEY,
    MODULE,
    FILE_PATH,
    DATA,
    LAST_MODIFIED
) VALUES (
    'storehoursnd21214306162.xml',
    'storehoursnd21214306162',
    'buildertabs',
    '/tables/buildertabs/storehoursnd21214306162.xml',
    '<?xml version="1.0" encoding="UTF-8"?><table>
    <connection-name>appnetix</connection-name>
    <table-name>_STOREHOURSND2_1214306162</table-name>
    <is-build-table>false</is-build-table>
    <form-builder-id/>
    <table-display-name>Store Timings</table-display-name>
    <table-header-map>
    <header name="bSec_storetimings1282868853" order="8" value="Store Timings"><type>0</type><section>bSec_storetimings1282868853</section><is-build-section>false</is-build-section></header></table-header-map>
    <id-field>idField</id-field>
    <field summary="true">
        <field-name>idField</field-name>
        <display-name>ID</display-name>
        <db-field>ID_FIELD</db-field>
        <data-type>Integer</data-type>
    </field>
    <foreign-tables>
        <foreign-table name="fimDocuments" table-export="false">
            <link-field foreignField="tabPrimaryId" thisField="idField"/>
            <link-field foreignField="entityID" thisField="entityID"/>
        </foreign-table>
    </foreign-tables>
    <field summary="true">
        <field-name>entityID</field-name>
        <display-name>Entity ID</display-name>
        <db-field>ENTITY_ID</db-field>
        <data-type>Integer</data-type>
    </field>
    <field summary="true">
        <field-name>tabPrimaryId</field-name>
        <display-name>Tab Primary Id</display-name>
        <db-field>TAB_PRIMARY_ID</db-field>
        <data-type>Integer</data-type>
    </field>
    <field summary="true">
        <field-name>_weekDays1642364703</field-name>
        <display-name>Week Days</display-name>
        <db-field>_WEEK_DAYS_1642364703</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>0</order-by>
        <mailmerge is-active="true" keyword-name="$storetimi_weekdays$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_closeAllDaymonday1541029386</field-name>
        <display-name>Close All Day</display-name>
        <db-field>_CLOSE_ALL_DAYMONDAY_1541029386</db-field>
        <data-type>String</data-type>
        <display-type>Radio</display-type>
        <group-by>true</group-by>
        <field-option_view>0</field-option_view>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>1</order-by>
        <mailmerge is-active="true" keyword-name="$storetimi_closeallday$"/>
        <pii-enabled>false</pii-enabled>
        <dependent>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_fromHours21378474792\', \'\', \'normal\')" name="_fromHours21378474792"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_toHours1750520942\', \'\', \'normal\')" name="_toHours1750520942"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_afternoonFromHours957746624\', \'\', \'normal\')" name="_afternoonFromHours957746624"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_afternoonToHours1923254276\', \'\', \'normal\')" name="_afternoonToHours1923254276"/>
        </dependent>
    </field>
    <field summary="true">
        <field-name>_fromHours21378474792</field-name>
        <display-name>Morning From Hours</display-name>
        <db-field>_FROM_HOURS2_1378474792</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>2</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_morningfromhours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_toHours1750520942</field-name>
        <display-name>Morning To Hours</display-name>
        <db-field>_TO_HOURS_1750520942</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>3</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_morningtohours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_afternoonFromHours957746624</field-name>
        <display-name>Afternoon From Hours</display-name>
        <db-field>_AFTERNOON_FROM_HOURS_957746624</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>4</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_afternoonfromhours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_afternoonToHours1923254276</field-name>
        <display-name>Afternoon To Hours</display-name>
        <db-field>_AFTERNOON_TO_HOURS_1923254276</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>5</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_afternoontohours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
</table>',
    '2025-07-02 13:14:55'
);



INSERT INTO CLIENT_XMLS (
    NAME,
    XML_KEY,
    MODULE,
    FILE_PATH,
    DATA,
    LAST_MODIFIED
) VALUES (
    'storehoursnd21214306162_copy.xml',
    'storehoursnd21214306162_copy',
    'buildertabs',
    '/tables/buildertabs/storehoursnd21214306162_copy.xml',
    '<?xml version="1.0" encoding="UTF-8"?><table>
    <connection-name>appnetix</connection-name>
    <table-name>_STOREHOURSND2_1214306162</table-name>
    <is-build-table>false</is-build-table>
    <form-builder-id/>
    <table-display-name>Store Timings</table-display-name>
    <table-header-map>
    <header name="bSec_storetimings1282868853" order="8" value="Store Timings"><type>0</type><section>bSec_storetimings1282868853</section><is-build-section>false</is-build-section></header></table-header-map>
    <id-field>idField</id-field>
    <field summary="true">
        <field-name>idField</field-name>
        <display-name>ID</display-name>
        <db-field>ID_FIELD</db-field>
        <data-type>Integer</data-type>
    </field>
    <foreign-tables>
        <foreign-table name="fimDocuments" table-export="false">
            <link-field foreignField="tabPrimaryId" thisField="idField"/>
            <link-field foreignField="entityID" thisField="entityID"/>
        </foreign-table>
    </foreign-tables>
    <field summary="true">
        <field-name>entityID</field-name>
        <display-name>Entity ID</display-name>
        <db-field>ENTITY_ID</db-field>
        <data-type>Integer</data-type>
    </field>
    <field summary="true">
        <field-name>tabPrimaryId</field-name>
        <display-name>Tab Primary Id</display-name>
        <db-field>TAB_PRIMARY_ID</db-field>
        <data-type>Integer</data-type>
    </field>
    <field summary="true">
        <field-name>_weekDays1642364703</field-name>
        <display-name>Week Days</display-name>
        <db-field>_WEEK_DAYS_1642364703</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>0</order-by>
        <mailmerge is-active="true" keyword-name="$storetimi_weekdays$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_closeAllDaymonday1541029386</field-name>
        <display-name>Close All Day</display-name>
        <db-field>_CLOSE_ALL_DAYMONDAY_1541029386</db-field>
        <data-type>String</data-type>
        <display-type>Radio</display-type>
        <group-by>true</group-by>
        <field-option_view>0</field-option_view>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>1</order-by>
        <mailmerge is-active="true" keyword-name="$storetimi_closeallday$"/>
        <pii-enabled>false</pii-enabled>
        <dependent>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_fromHours21378474792\', \'\', \'normal\')" name="_fromHours21378474792"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_toHours1750520942\', \'\', \'normal\')" name="_toHours1750520942"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_afternoonFromHours957746624\', \'\', \'normal\')" name="_afternoonFromHours957746624"/>
            <dependent-child function="getCustomCombo(\'dataCombo\', this, \'_afternoonToHours1923254276\', \'\', \'normal\')" name="_afternoonToHours1923254276"/>
        </dependent>
    </field>
    <field summary="true">
        <field-name>_fromHours21378474792</field-name>
        <display-name>Morning From Hours</display-name>
        <db-field>_FROM_HOURS2_1378474792</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>2</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_morningfromhours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_toHours1750520942</field-name>
        <display-name>Morning To Hours</display-name>
        <db-field>_TO_HOURS_1750520942</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>3</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_morningtohours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_afternoonFromHours957746624</field-name>
        <display-name>Afternoon From Hours</display-name>
        <db-field>_AFTERNOON_FROM_HOURS_957746624</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>4</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_afternoonfromhours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
    <field summary="true">
        <field-name>_afternoonToHours1923254276</field-name>
        <display-name>Afternoon To Hours</display-name>
        <db-field>_AFTERNOON_TO_HOURS_1923254276</db-field>
        <data-type>String</data-type>
        <display-type>Combo</display-type>
        <group-by>true</group-by>
        <section>bSec_storetimings1282868853</section>
        <is-active>yes</is-active>
        <is-mandatory>false</is-mandatory>
        <build-field>no</build-field>
        <field-export>true</field-export>
        <order-by>5</order-by>
        <dropdown-option>3</dropdown-option>
        <dependent-parent>_closeAllDaymonday1541029386##Radio##false</dependent-parent>
        <mailmerge is-active="true" keyword-name="$storetimi_afternoontohours$"/>
        <pii-enabled>false</pii-enabled>
    </field>
</table>',
    '2025-07-02 13:14:55'
);





INSERT INTO TABULAR_SECTION_DISPLAY_COLUMN (
    DISPLAY_NAME,
    DISPLAY_VALUE,
    MODULE_ID,
    IS_SELECTED,
    ORDER_SEQUENCE,
    USER_NO,
    IS_ACTIVE,
    FIELD_NAME,
    IS_PII_ENABLED,
    TABLE_NAME,
    MAIN_TABLE_NAME
) VALUES
('Week Days', '_WEEK_DAYS_1642364703', 3, 1, 1, 0, 'yes', '_weekDays1642364703', false, 'storehoursnd21214306162', 'franchisees'),
('Close All Day', '_CLOSE_ALL_DAYMONDAY_1541029386', 3, 1, 2, 0, 'yes', '_closeAllDaymonday1541029386', false, 'storehoursnd21214306162', 'franchisees'),
('Morning From Hours', '_FROM_HOURS2_1378474792', 3, 1, 3, 0, 'yes', '_fromHours21378474792', false, 'storehoursnd21214306162', 'franchisees'),
('Morning To Hours', '_TO_HOURS_1750520942', 3, 1, 4, 0, 'yes', '_toHours1750520942', false, 'storehoursnd21214306162', 'franchisees'),
('Afternoon From Hours', '_AFTERNOON_FROM_HOURS_957746624', 3, 1, 5, 0, 'yes', '_afternoonFromHours957746624', false, 'storehoursnd21214306162', 'franchisees'),
('Afternoon To Hours', '_AFTERNOON_TO_HOURS_1923254276', 3, 1, 6, 0, 'yes', '_afternoonToHours1923254276', false, 'storehoursnd21214306162', 'franchisees');

INSERT INTO FORM_FIELD_ACCESS_MAPPING (
    FIELD_NAME,
    ACCESS_TO_ALL,
    TABLE_ANCHOR,
    FORM_ID,
    MODULE_ID,
    IS_ACTIVE,
    CREATED_ON,
    CREATED_BY,
    MODIFIED_BY,
    MODIFIED_ON,
    SOLR_LAST_UPDATED
) VALUES
('_weekDays1642364703', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:28:00', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.569'),
('_closeAllDaymonday1541029386', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:29:22', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.497'),
('_fromHours21378474792', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:30:50', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.458'),
('_toHours1750520942', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:31:29', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.530'),
('_afternoonFromHours957746624', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:32:04', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.601'),
('_afternoonToHours1923254276', 'yes', 'storehoursnd21214306162', 1, 3, 'Y', '2025-07-02 10:32:39', 1, NULL, '2025-07-02 12:50:59', '2025-07-02 12:50:59.641');

INSERT INTO TRIGGER_EVENT (
    TRIGGER_ID,
    TRIGGER_ONOFF,
    AUDITING_ONOFF,
    TABLE_NAME,
    FIELD_NAME,
    EVENT,
    EMAIL_ID,
    USER_NO,
    ALERT_MESSAGE,
    DAYS_PRIOR,
    VALUE_TO_COMPARE,
    DB_TABLE_NAME,
    FIELD_ORDER,
    DB_FIELD_NAME,
    DISPLAY_FIELD_NAME,
    ACTUAL_DATA_TYPE,
    PARENT_EVENT_ID,
    CHILD_TRIGGER_ORDER,
    IS_TABULAR_SECTION
) VALUES
(1, 0, 1, 'storehoursnd21214306162', '_weekDays1642364703', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777781, '_WEEK_DAYS_1642364703', 'Week Days', 'String', NULL, 0, 'Y'),
(1, 0, 1, 'storehoursnd21214306162', '_closeAllDaymonday1541029386', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777782, '_CLOSE_ALL_DAYMONDAY_1541029386', 'Close All Day', 'String', NULL, 0, 'Y'),
(1, 0, 1, 'storehoursnd21214306162', '_fromHours21378474792', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777783, '_FROM_HOURS2_1378474792', 'Morning From Hours', 'String', NULL, 0, 'Y'),
(1, 0, 1, 'storehoursnd21214306162', '_toHours1750520942', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777784, '_TO_HOURS_1750520942', 'Morning To Hours', 'String', NULL, 0, 'Y'),
(1, 0, 1, 'storehoursnd21214306162', '_afternoonFromHours957746624', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777785, '_AFTERNOON_FROM_HOURS_957746624', 'Afternoon From Hours', 'String', NULL, 0, 'Y'),
(1, 0, 1, 'storehoursnd21214306162', '_afternoonToHours1923254276', '', '', 0, '', 0, '', '_STOREHOURSND2_1214306162', 777786, '_AFTERNOON_TO_HOURS_1923254276', 'Afternoon To Hours', 'String', NULL, 0, 'Y');



CREATE TABLE `_STOREHOURSND2_1214306162` (
  `ID_FIELD` int NOT NULL AUTO_INCREMENT,
  `TAB_PRIMARY_ID` int DEFAULT NULL,
  `CREATION_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ENTITY_ID` int DEFAULT NULL,
  `FRANCHISEE_NO` int DEFAULT NULL,
  `_WEEK_DAYS_1642364703` int DEFAULT NULL,
  `_FROM_MIN2_899389445` int DEFAULT NULL,
  `_TO_HOURS_1750520942` int DEFAULT NULL,
  `_TO_MINUTES_771513518` int DEFAULT NULL,
  `_CLOSE_ALL_DAYMONDAY_1541029386` int DEFAULT NULL,
  `_FROM_HOURS2_1378474792` int DEFAULT NULL,
  `_AFTERNOON_FROM_HOURS_957746624` int DEFAULT NULL,
  `_AFTERNOON_FROM_MINUTES_935417333` int DEFAULT NULL,
  `_AFTERNOON_TO_HOURS_1923254276` int DEFAULT NULL,
  `_AFTERNOON_TO_MINUTES_1041996827` int DEFAULT NULL,
    `PROCESSBY` varchar(255) DEFAULT NULL,
  `TARGET_DATABASE` varchar(255) DEFAULT NULL,
  `TENANT_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID_FIELD`),
  KEY `TAB_PRIMARY_ID_IDX` (`TAB_PRIMARY_ID`),
  KEY `ENTITY_ID_IDX` (`ENTITY_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3;