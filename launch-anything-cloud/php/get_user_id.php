<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');

// check if a user exists in the database
include('require_user_name_exist.php');

// get the user id
$row = $result->fetch_assoc();
$user_id = $row['id'];
