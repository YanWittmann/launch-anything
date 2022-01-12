<?php

include_once('util.php');
include_once('connect_database.php');

$username = post_or_get_or_die('username');

$tile_id = post_or_get_or_die('tile_id');
if (!is_uuid($tile_id)) {
    die_with_message_and_error('tile_id is not a valid UUID', $tile_id);
}
$tile_label = post_or_get('tile_label');
$tile_category = post_or_get('tile_category');
$tile_action = post_or_get('tile_action');
$tile_keywords = post_or_get('tile_keywords');

$data_to_update = array();
if ($tile_label != null) $data_to_update['label'] = $tile_label;
if ($tile_category != null) $data_to_update['category'] = $tile_category;
if ($tile_action != null) $data_to_update['action'] = $tile_action;
if ($tile_keywords != null) $data_to_update['keywords'] = $tile_keywords;


include('validate_login_data.php');
include('get_user_id.php');

// check if the tile id already exists
$query = "SELECT * FROM la_tiles WHERE id = '$tile_id'";
$result = $db->query($query);

// if the tile id already exists, modify the tile
if ($result->num_rows == 0) {
    // if the tile id does not exist, create the tile
    $query = "INSERT INTO la_tiles (id, user, label, category, action, keywords) VALUES ('$tile_id', '$user_id', '', '', '', '')";
    $result = $db->query($query);
    if (!$result) die_with_message_and_error('Failed to create tile.', $db->error);
} else {
    // check if the user is the owner of the tile
    $row = $result->fetch_assoc();
    if ($row['user'] != $user_id) die_with_message('You are not the owner of this tile.');
}

// now update the tile data with the passed data
$query = "UPDATE la_tiles SET ";
$i = 0;
foreach ($data_to_update as $key => $value) {
    if ($i > 0) $query .= ', ';
    $query .= "$key = '$value'";
    $result = $db->query($query);
    if (!$result) die_with_message_and_error('Failed to modify tile.', $db->error);
    $i++;
}

success_exit_with_message('Tile data modified.');
