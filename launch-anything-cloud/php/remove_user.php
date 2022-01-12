<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');

// check if a user exists in the database
include('require_user_name_exist.php');

include('validate_login_data.php');
include('get_user_id.php');

// delete the user from the database by removing his entry and all tile entries that have the user as owner
$query = "DELETE FROM la_tiles WHERE user = '$user_id'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Unable to delete user tiles.', $db->error);
}

// now delete the user from the database
$query = "DELETE FROM la_users WHERE id = '$user_id'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Unable to delete user.', $db->error);
}

success_exit_with_message('User deleted.');
