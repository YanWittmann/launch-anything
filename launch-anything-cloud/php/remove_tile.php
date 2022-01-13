<?php

include_once('util.php');
include_once('connect_database.php');

$tile_id = post_or_get_or_die('tile_id');
if (!is_uuid($tile_id)) {
    die_with_message_and_error('tile_id is not a valid UUID', $tile_id);
}


include('validate_login_data.php');
include('get_user_id.php');


// check if the user is the owner of the tile
$query = "SELECT user FROM la_tiles WHERE id = '$tile_id'";
$result = $db->query($query);
if (!$result) {
    die_with_message_and_error('Failed to query database', $query);
}
$row = $result->fetch_assoc();
if ($row['user'] != $user_id) {
    die_with_message('You are not the owner of this tile: ' . $row['user'] . ' != ' . $user_id);
}

// now delete the tile row
$query = "DELETE FROM la_tiles WHERE id = '$tile_id'";
$result = $db->query($query);

if (!$result) {
    die_with_message_and_error('Failed to delete tile', $db->error);
}

success_exit_with_message('Tile deleted');
