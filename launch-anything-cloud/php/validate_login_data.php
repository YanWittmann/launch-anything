<?php

include_once('util.php');
include_once('connect_database.php');

// check if the user exists and has the right password
include('require_user_name_exist.php');

// get the user data and check the password
$password = post_or_get_or_die('password');
$row = $result->fetch_assoc();
if (!password_verify($password, $row['password'])) {
    die_with_message('Invalid password');
}

if (post_or_get('success_response') === 'true') {
    success_exit_with_message('Login successful');
}