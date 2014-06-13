# read one line at a time and executed.
CREATE TABLE "APP"."CONFIG_PASSWORDS" (  CONFIG_ID bigint NOT NULL,    PASSWORD_ID int NOT NULL,    PRIMARY KEY (CONFIG_ID,PASSWORD_ID) ) 
CREATE TABLE "APP"."CONFIG_SEEDLISTS" (  CONFIG_ID bigint NOT NULL,    SEEDLIST_ID int NOT NULL,    PRIMARY KEY (CONFIG_ID,SEEDLIST_ID) ) 
CREATE TABLE "APP"."CONFIGURATIONS" (  CONFIG_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    DOMAIN_ID bigint NOT NULL,    TEMPLATE_ID bigint NOT NULL,    MAXRATE int,    OVERRIDELIMITS int,    MAXBYTES bigint DEFAULT -1,    MAXOBJECTS bigint DEFAULT -1 NOT NULL ) 
CREATE TABLE "APP"."DOMAINS" (  DOMAIN_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    DEFAULTCONFIG bigint NOT NULL,    CRAWLERTRAPS varchar(1000),    EDITION bigint NOT NULL,    ALIAS bigint,    LASTALIASUPDATE timestamp ) 
CREATE TABLE "APP"."EXTENDEDFIELD" (  EXTENDEDFIELD_ID bigint PRIMARY KEY NOT NULL  GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    EXTENDEDFIELDTYPE_ID bigint NOT NULL,    NAME varchar(50) NOT NULL,    FORMAT varchar(50) NOT NULL,    DEFAULTVALUE varchar(50) NOT NULL,    OPTIONS varchar(50) NOT NULL,    DATATYPE int NOT NULL,    MANDATORY int NOT NULL,    SEQUENCENR int ) 
CREATE TABLE "APP"."EXTENDEDFIELDTYPE" (  EXTENDEDFIELDTYPE_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(50) NOT NULL ) 
CREATE TABLE "APP"."EXTENDEDFIELDVALUE" (  EXTENDEDFIELDVALUE_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    EXTENDEDFIELD_ID bigint NOT NULL,    INSTANCE_ID bigint NOT NULL,    CONTENT varchar(100) NOT NULL ) 
CREATE TABLE "APP"."FRONTIERREPORTMONITOR" (  JOBID bigint NOT NULL,    FILTERID varchar(200) NOT NULL,    TSTAMP timestamp NOT NULL,    DOMAINNAME varchar(300) NOT NULL,    CURRENTSIZE bigint NOT NULL,    TOTALENQUEUES bigint NOT NULL,    SESSIONBALANCE bigint NOT NULL,    LASTCOST bigint NOT NULL,    AVERAGECOST bigint NOT NULL,    LASTDEQUEUETIME varchar(100) NOT NULL,    WAKETIME varchar(100) NOT NULL,    TOTALSPEND bigint NOT NULL,    TOTALBUDGET bigint NOT NULL,    ERRORCOUNT bigint NOT NULL,    LASTPEEKURI varchar(1000) NOT NULL,    LASTQUEUEDURI varchar(1000) NOT NULL ) 
CREATE TABLE "APP"."FULLHARVESTS" (  HARVEST_ID bigint PRIMARY KEY NOT NULL,    MAXOBJECTS bigint NOT NULL,    PREVIOUSHD bigint,    MAXBYTES bigint DEFAULT -1 NOT NULL,    MAXJOBRUNNINGTIME bigint DEFAULT 0 NOT NULL,    ISINDEXREADY int DEFAULT 0 NOT NULL ) 
CREATE TABLE "APP"."GLOBAL_CRAWLER_TRAP_EXPRESSIONS" (  CRAWLER_TRAP_LIST_ID int NOT NULL  GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    TRAP_EXPRESSION varchar(1000) NOT NULL,    PRIMARY KEY (CRAWLER_TRAP_LIST_ID,TRAP_EXPRESSION) ) 
CREATE TABLE "APP"."GLOBAL_CRAWLER_TRAP_LISTS" (  GLOBAL_CRAWLER_TRAP_LIST_ID int PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    DESCRIPTION long varchar,    ISACTIVE int NOT NULL ) 
CREATE TABLE "APP"."HARVEST_CONFIGS" (  HARVEST_ID bigint NOT NULL,    CONFIG_ID bigint NOT NULL, PRIMARY KEY (HARVEST_ID,CONFIG_ID) ) 
# HARVESTCHANNEL ?
CREATE TABLE "APP"."HARVESTDEFINITIONS" (  HARVEST_ID bigint PRIMARY KEY NOT NULL,    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    NUMEVENTS int NOT NULL,    SUBMITTED timestamp NOT NULL,    ISACTIVE int NOT NULL,    EDITION bigint NOT NULL ) 
CREATE TABLE "APP"."HISTORYINFO" (  HISTORYINFO_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    STOPREASON int NOT NULL,    OBJECTCOUNT bigint NOT NULL,    BYTECOUNT bigint NOT NULL,    CONFIG_ID bigint NOT NULL,    HARVEST_ID bigint NOT NULL,    JOB_ID bigint,    HARVEST_TIME timestamp NOT NULL ) 
CREATE TABLE "APP"."JOB_CONFIGS" (  JOB_ID bigint NOT NULL,    CONFIG_ID bigint NOT NULL,    PRIMARY KEY (JOB_ID,CONFIG_ID) ) 
CREATE TABLE "APP"."JOBS" (  JOB_ID bigint PRIMARY KEY NOT NULL,    HARVEST_ID bigint NOT NULL,    STATUS int NOT NULL,    PRIORITY int NOT NULL,    FORCEMAXBYTES bigint DEFAULT -1 NOT NULL,    FORCEMAXCOUNT bigint,    ORDERXML varchar(300) NOT NULL,    ORDERXMLDOC clob(67108864) NOT NULL,    SEEDLIST clob(67108864) NOT NULL,    HARVEST_NUM int NOT NULL,    HARVEST_ERRORS varchar(300),    HARVEST_ERROR_DETAILS long varchar,    UPLOAD_ERRORS varchar(300),    UPLOAD_ERROR_DETAILS long varchar,    STARTDATE timestamp,    ENDDATE timestamp,    NUM_CONFIGS int DEFAULT 0 NOT NULL,    EDITION bigint NOT NULL,    SUBMITTEDDATE timestamp,    RESUBMITTED_AS_JOB bigint,    FORCEMAXRUNNINGTIME bigint DEFAULT 0 NOT NULL,    CONTINUATIONOF bigint DEFAULT NULL,    CREATIONDATE timestamp DEFAULT NULL ) 
CREATE TABLE "APP"."ORDERTEMPLATES" (  TEMPLATE_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    ORDERXML clob(67108864) NOT NULL ) 
CREATE TABLE "APP"."OWNERINFO" (  OWNERINFO_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    DOMAIN_ID bigint NOT NULL,    CREATED timestamp NOT NULL,    INFO varchar(1000) NOT NULL ) 
CREATE TABLE "APP"."PARTIALHARVESTS" (  HARVEST_ID bigint PRIMARY KEY NOT NULL,    SCHEDULE_ID bigint NOT NULL,    NEXTDATE timestamp ) 
CREATE TABLE "APP"."PASSWORDS" (  PASSWORD_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    DOMAIN_ID bigint NOT NULL,    URL varchar(300) NOT NULL,    REALM varchar(300) NOT NULL,    USERNAME varchar(20) NOT NULL,    PASSWORD varchar(40) NOT NULL ) 
CREATE TABLE "APP"."RUNNINGJOBSHISTORY" (  JOBID bigint NOT NULL,    HARVESTNAME varchar(300) NOT NULL,    HOSTURL varchar(300) NOT NULL,    PROGRESS bigint NOT NULL,    QUEUEDFILESCOUNT bigint NOT NULL,    TOTALQUEUESCOUNT bigint NOT NULL,    ACTIVEQUEUESCOUNT bigint NOT NULL,    EXHAUSTEDQUEUESCOUNT bigint NOT NULL,    ELAPSEDSECONDS bigint NOT NULL,    ALERTSCOUNT bigint NOT NULL,    DOWNLOADEDFILESCOUNT bigint NOT NULL,    CURRENTPROCESSEDKBPERSEC int NOT NULL,    PROCESSEDKBPERSEC int NOT NULL,    CURRENTPROCESSEDDOCSPERSEC bigint NOT NULL,    PROCESSEDDOCSPERSEC bigint NOT NULL,    ACTIVETOECOUNT int NOT NULL,    STATUS int NOT NULL,    TSTAMP timestamp NOT NULL,    RETIREDQUEUESCOUNT bigint DEFAULT 0 NOT NULL, PRIMARY KEY (JOBID,HARVESTNAME,ELAPSEDSECONDS,TSTAMP) ) 
CREATE TABLE "APP"."RUNNINGJOBSMONITOR" (  JOBID bigint NOT NULL,    HARVESTNAME varchar(300) NOT NULL,    HOSTURL varchar(300) NOT NULL,    PROGRESS bigint NOT NULL,    QUEUEDFILESCOUNT bigint NOT NULL,    TOTALQUEUESCOUNT bigint NOT NULL,    ACTIVEQUEUESCOUNT bigint NOT NULL,    EXHAUSTEDQUEUESCOUNT bigint NOT NULL,    ELAPSEDSECONDS bigint NOT NULL,    ALERTSCOUNT bigint NOT NULL,    DOWNLOADEDFILESCOUNT bigint NOT NULL,    CURRENTPROCESSEDKBPERSEC int NOT NULL,    PROCESSEDKBPERSEC int NOT NULL,    CURRENTPROCESSEDDOCSPERSEC bigint NOT NULL,    PROCESSEDDOCSPERSEC bigint NOT NULL,    ACTIVETOECOUNT int NOT NULL,    STATUS int NOT NULL,    TSTAMP timestamp NOT NULL,    RETIREDQUEUESCOUNT bigint DEFAULT 0 NOT NULL,  PRIMARY KEY (JOBID,HARVESTNAME) ) 
CREATE TABLE "APP"."SCHEDULES" (  SCHEDULE_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    STARTDATE timestamp,    ENDDATE timestamp,    MAXREPEATS bigint,    TIMEUNIT int NOT NULL,    NUMTIMEUNITS bigint NOT NULL,    ANYTIME int NOT NULL,    ONMINUTE int,    ONHOUR int,    ONDAYOFWEEK int,    ONDAYOFMONTH int,    EDITION bigint NOT NULL ) 
CREATE TABLE "APP"."SCHEMAVERSIONS" (  TABLENAME varchar(100) NOT NULL,    VERSION int NOT NULL ) 
CREATE TABLE "APP"."SEEDLISTS" (  SEEDLIST_ID bigint PRIMARY KEY NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),    NAME varchar(300) NOT NULL,    COMMENTS long varchar,    DOMAIN_ID bigint NOT NULL,    SEEDS clob(8388608) NOT NULL ) 

