<?php

include_once('util.php');
include_once('connect_database.php');

// check if the user exists and has the right password
$query = "SELECT * FROM la_users WHERE name='$username'";
$result = $db->query($query);
if ($result->num_rows == 0) {
    die_with_message('Invalid username');
}

// get the user data and check the password
$password = post_or_get_or_die('password');
$row = $result->fetch_assoc();
if (!password_verify($password, $row['password'])) {
    die_with_message('Invalid password ' . $password . ' ' . $row['password']);
}
