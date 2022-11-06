
-- Paperless app data base tables
--

create table PERSONAL_GENERIC_TRANSACTION(
    txn_seq int(8) not null auto_increment primary key,
    user_id int(8) not null,
    amount float default 0.0,
    txn_date bigint,
    txn_type int(8), -- transaction type
    txn_type_level int(4), -- level 0, level 1, level 2
    txn_source varchar(10), -- sms, manual
    txn_mode varchar(10), -- debit, credit
    paid_by varchar(10), -- cash, online
    is_source_correct bool,
    txn_title varchar(150) not null, -- transaction name
    budget_id int(8) default 0 -- budget id for the transaction
)

create table PERSONAL_TRANSACTION_TYPE(
    type_seq int(8) not null auto_increment primary key,
    expense_type_title varchar(150) not null,
    txn_level int(4) not null, -- transaction level like amazon online
    parent_level int(4) default 0, -- transaction parent level grocery
    icon varchar(50),
    added_by int(8) default 0
)

create table PERSONAL_TRANSACTION_SUMMARY(
    summary_seq int(8) not null auto_increment primary key,
    summary_for varchar(10) not null, -- mm_yyyy -- month year
    user_id int(8) not null, -- summary for this user
    txn_type int(8), -- transaction type
    txn_type_level int(4), -- level 0, level 1, level 2
    total_amount float,

)

create table PERSONAL_GOAL_SUMMARY(
    goal_seq int(8) not null auto_increment primary key,
    goal_title varchar(150) not null,
    goal_desc varchar(500), -- description for goal
    user_id int(8) not null,
    target_amt float,
    monthly_installment float,
    start_month_year varchar(10),  -- mm_yyyy  10_2022
    total_month int(4),
    date_added bigint, -- date when goal is created
    last_payment int(4)
)
create table PERSONAL_BUDGET_SUMMARY(
    budget_seq int(8) not null auto_increment primary key,
    budget_for varchar(10) not null, --mm_yyyy 10_2022
    budget_title varchar(100) not null,
    user_id int(8) not null,
    budget_amount float, -- 0.0
    date_added bigint,
    txn_type int(8),
    txn_type_level int(4) -- level 0, level 1, level 2
)

create table PERSONAL_UPCOMING_EXPENSE(
    expense_seq int(8) not null auto_increment primary key,
    user_id int(8) not null,
    expense_title varchar(150),
    source varchar(10),
    amount float,
    txn_for varchar(10), -- mm_yyyy ,
    due_date bigint,
    date_produced bigint,
    is_paid bool default false,
    is_valid bool default true,
    is_goal_based_expense bool default false,
    budget_seq int(8) -- goal based or fixed budget
)


-- user and maintenance

create table LOGIN_USER(
    user_id int(8) not null auto_increment primary key,
    email varchar(150) not null,
    password varchar(200) not null,
    login_type varchar(10) default "password" -- password, fingerprint, faceid
)

create table USER_PROFILE(
    user_id int(8) not null,
    user_name varchar(150) not null,
    email varchar(150) not null,
    user_image varchar(100),
    country varchar(2),
    city varchar(100)
)

create table USER_DEVICE_MAPPING(
    mapping_seq int(8) not null auto_increment primary key,
    user_id int(8) not null,
    device_id varchar(50),
    pns_id varchar(255),
    device_name varchar(100),
    device_type varchar(10), --android, iphone
    date_added bigint,
    is_active bool default true
)

-- cms and image management

create table DOCUMENT_TABLE(
    document_seq int(8) not null auto_increment primary key,
    generic_id int(8) not null,
    document_type varchar(4) not null,
    add_date bigint,

)