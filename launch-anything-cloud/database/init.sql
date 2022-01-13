CREATE TABLE la_tiles (
    id char(36) NOT NULL,
    user char(36) NOT NULL,
    label varchar(45) NOT NULL,
    category varchar(45) NOT NULL,
    action varchar(300) NOT NULL,
    keywords varchar(45) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE la_users (
    id char(36) NOT NULL,
    name varchar(45) NOT NULL,
    password varchar(255) NOT NULL,
    PRIMARY KEY (id)
);