<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');
$new_password = post_or_get_or_die('new_password');

// check if new_username is valid
if (!is_valid_password($new_password)) {
    die_with_message('Invalid password, must be 8 characters long and contain at least one number, one uppercase letter, one lowercase letter and no spaces');
}

$new_password = hash_pwd($new_password);

include('require_user_name_exist.php');
include('validate_login_data.php');

// now update the user name
$query = "UPDATE la_users SET password='$new_password' WHERE name='$username'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Failed to update user password', $db->error);
}

success_exit_with_message('User password updated');
