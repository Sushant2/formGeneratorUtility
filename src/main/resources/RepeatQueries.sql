UPDATE FS_WORKFLOW SET STATUS = 0 WHERE MODULE = 'fs';
INSERT INTO USER_TAB_CONFIG 
(USER_TAB_ID, ROW_NO, MODULE, SUB_MODULE, HREF, KEY_NAME, NAME, PATH, INTERNAL_NAME, IS_CUSTOM, IS_ACTIVE_MBE, ARCHIVE_TABLE, FRANCHISEE_HREF)
SELECT NULL, 1, 'fim', 'franchiseewithoutsc', 'logCall', 'noKey', 'Call', '', 'Call', 'N', 'Y', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM USER_TAB_CONFIG 
    WHERE MODULE='fim' 
      AND SUB_MODULE='franchiseewithoutsc' 
      AND HREF='logCall'
);

INSERT INTO USER_TAB_CONFIG 
(USER_TAB_ID, ROW_NO, MODULE, SUB_MODULE, HREF, KEY_NAME, NAME, PATH, INTERNAL_NAME, IS_CUSTOM, IS_ACTIVE_MBE, ARCHIVE_TABLE, FRANCHISEE_HREF)
SELECT NULL, 1, 'fim', 'franchiseewithoutsc', 'fimRemarks', 'noKey', 'Remark', '', 'Remark', 'N', 'Y', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM USER_TAB_CONFIG 
    WHERE MODULE='fim' 
      AND SUB_MODULE='franchiseewithoutsc' 
      AND HREF='fimRemarks'
);

INSERT INTO USER_TAB_CONFIG 
(USER_TAB_ID, ROW_NO, MODULE, SUB_MODULE, HREF, KEY_NAME, NAME, PATH, INTERNAL_NAME, IS_CUSTOM, IS_ACTIVE_MBE, ARCHIVE_TABLE, FRANCHISEE_HREF)
SELECT NULL, 1, 'fim', 'franchiseewithoutsc', 'logTasks', 'noKey', 'Task', '', 'Task', 'N', 'Y', NULL, NULL
WHERE NOT EXISTS (
    SELECT 1 FROM USER_TAB_CONFIG 
    WHERE MODULE='fim' 
      AND SUB_MODULE='franchiseewithoutsc' 
      AND HREF='logTasks'
);
UPDATE FRANCHISEE SET AREA_ID = AF_ID;
UPDATE CLIENT_LANG_PROPS SET PROP_VALUE='Open' WHERE PROP_KEY= 'Active';
UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<display-name>Current Status</display-name>', '<display-name>Transfer status</display-name>') WHERE XML_KEY IN ('fimTransfer', 'fimTransfer_copy');
UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<display-name>Current Status</display-name>', '<display-name>Standby / Completed</display-name>') WHERE XML_KEY IN ('fimRenewal', 'fimRenewal_copy');

UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<sync-with sync-module="within">fsLeadDetails##noteForQD##false</sync-with>', '<sync-with sync-module="within">fsLeadDetails##_noteForQD##false</sync-with>') WHERE XML_KEY = 'fsLeadQualificationDetail';
 
UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<sync-with sync-module="within">fsLeadDetails##leadQualificationFrom##false</sync-with>', '<sync-with sync-module="within">fsLeadDetails##_leadQualificationFrom##false</sync-with>') WHERE XML_KEY = 'fsLeadQualificationDetail';
 
UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<sync-with sync-module="within">fsLeadDetails##birthdate##false</sync-with>', '<sync-with sync-module="within">fsLeadDetails##_birthdate##false</sync-with>') WHERE XML_KEY = 'fsLeadQualificationDetail';

UPDATE CLIENT_XMLS SET DATA = REPLACE(DATA, '<display-name>FBC</display-name>', '<display-name>Supervisor</display-name>') WHERE XML_KEY IN ('franchisees', 'franchisees_copy');