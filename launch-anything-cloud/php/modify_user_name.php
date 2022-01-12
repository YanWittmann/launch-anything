<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');
$new_username = post_or_get_or_die('new_username');

// check if new_username is valid
if (!is_valid_username($new_username)) {
    die_with_message('Invalid new username ' . $new_username);
}

// check if a user exists in the database
include('require_user_name_exist.php');

include('validate_login_data.php');

// now update the user name
$query = "UPDATE la_users SET name='$new_username' WHERE name='$username'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Failed to update user name', $db->error);
}

success_exit_with_message('User name updated to ' . $new_username);
