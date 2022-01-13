<?php

include_once('util.php');
include_once('connect_database.php');

$old_username = post_or_get_or_die('username');
$new_username = post_or_get_or_die('new_username');

// check if new_username is valid
if (!is_valid_username($new_username)) {
    die_with_message('Invalid new username ' . $new_username);
}

// check if a user exists in the database
include('require_user_name_exist.php');

include('validate_login_data.php');

// check if the new user id already exists
$query = "SELECT * FROM la_users WHERE name='$new_username'";
$result = $db->query($query);
if ($result->num_rows > 0) {
    die_with_message_and_error('User already exists', $new_username);
}

// now update the user name
$query = "UPDATE la_users SET name='$new_username' WHERE name='$old_username'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Failed to update user name', $db->error);
}

success_exit_with_message('User name updated to ' . $new_username);
