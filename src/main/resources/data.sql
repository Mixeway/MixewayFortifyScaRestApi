DROP TABLE IF EXISTS fortifyscan;

create table fortifyscan(
  id serial primary key,
  groupname text,
  projectname text,
  requestid text,
  scanid text,
  inqueue boolean,
  running boolean,
  technique text,
  commitid text,
  error boolean

);
