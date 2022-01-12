<?php

include_once('util.php');
include_once('connect_database.php');

$user_id = uuid();
$username = post_or_get_or_die('username');
$password = hash_pwd(post_or_get_or_die('password'));

// this php file will create a new user in the database
// start by checking if the user is already in the database
$query = "SELECT * FROM la_users WHERE name='$username'";
$result = $db->query($query);
if ($result->num_rows > 0) {
    // user already exists
    die_with_message_and_error('User already exists', $username);
}

// user does not exist, create it
$query = "INSERT INTO la_users (id, name, password) VALUES ('$user_id', '$username', '$password')";
$result = $db->query($query);
if ($result) {
    success_exit_with_message('User created');
} else {
    die_with_message_and_error('Error creating user', $db->error);
}
