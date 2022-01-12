<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');

// check if a user exists in the database
$query = "SELECT * FROM la_users WHERE name='$username'";
$result = $db->query($query);
if ($result->num_rows == 0) {
    die_with_message_and_error('User does not exist', $username);
}

// get the user id
$row = $result->fetch_assoc();
$user_id = $row['id'];
