<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');

include('validate_login_data.php');
include('get_user_id.php');

// find the user's tiles in la_tiles
$query = "SELECT * FROM la_tiles WHERE user = '$user_id'";
$result = $db->query($query);

if (!$result) {
    die_with_message_and_error('Unable to retrieve tile data.', $db->error);
}

if ($result->num_rows == 0) {
    die_with_message('No tiles found.');
}

// iterate through the tiles and return them as a JSON object
$tiles = array();
while ($row = $result->fetch_assoc()) {
    $tile = array();
    $tile['id'] = $row['id'];
    $tile['label'] = $row['label'];
    $tile['category'] = $row['category'];
    $tile['action'] = $row['action'];
    $tile['keywords'] = $row['keywords'];
    array_push($tiles, $tile);
}

success_exit_with_message(json_encode($tiles));
