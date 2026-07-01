create table READING_EVENT
(
    ID         varchar  primary key,
    USER_ID    varchar  not null,
    BOOK_ID    varchar  not null,
    SERIES_ID  varchar  not null,
    TYPE       varchar  not null,
    CREATED_AT datetime not null default current_timestamp,
    foreign key (USER_ID) references USER (ID) on delete cascade,
    foreign key (BOOK_ID) references BOOK (ID) on delete cascade,
    foreign key (SERIES_ID) references SERIES (ID) on delete cascade
);

create index idx_reading_event_series_id on READING_EVENT (SERIES_ID);
create index idx_reading_event_user_id on READING_EVENT (USER_ID);
create index idx_reading_event_created_at on READING_EVENT (CREATED_AT);

create table SERIES_READING_STATS_DAILY
(
    SERIES_ID       varchar  not null,
    DAY             date     not null,
    UNIQUE_READERS  integer  not null default 0,
    COMPLETED_BOOKS integer  not null default 0,
    PROGRESS_EVENTS integer  not null default 0,
    LAST_READ_AT    datetime null,
    primary key (SERIES_ID, DAY),
    foreign key (SERIES_ID) references SERIES (ID) on delete cascade
);

create index idx_series_reading_stats_daily_day on SERIES_READING_STATS_DAILY (DAY);
