<?php

function die_with_message($message) {
    echo json_encode(array('code' => 'error', 'message' => $message));
    exit(1);
}

function die_with_message_and_error($message, $error) {
    echo json_encode(array('code' => 'error', 'message' => $message, 'error' => $error));
    exit(1);
}

function success_exit_with_message($message) {
    echo json_encode(array('code' => 'success', 'message' => $message));
    exit(0);
}

function post_or_get_or_die($var_name) {
    if (isset($_POST[$var_name])) {
        $var = $_POST[$var_name];
        if (isset($db)) $var = $db->real_escape_string($var);
        return $var;
    } else if (isset($_GET[$var_name])) {
        $var = $_GET[$var_name];
        if (isset($db)) $var = $db->real_escape_string($var);
        return $var;
    } else {
        die_with_message("Missing parameter: $var_name");
    }
    return null;
}

function post_or_get($var_name) {
    if (isset($_POST[$var_name])) {
        $var = $_POST[$var_name];
        if (isset($db)) $var = $db->real_escape_string($var);
        return $var;
    } else if (isset($_GET[$var_name])) {
        $var = $_GET[$var_name];
        if (isset($db)) $var = $db->real_escape_string($var);
        return $var;
    }
    return null;
}

function hash_pwd($password) {
    return password_hash($password, PASSWORD_DEFAULT);
}

function uuid() {
    return sprintf('%04x%04x-%04x-%04x-%04x-%04x%04x%04x',

        // 32 bits for "time_low"
        mt_rand(0, 0xffff), mt_rand(0, 0xffff),

        // 16 bits for "time_mid"
        mt_rand(0, 0xffff),

        // 16 bits for "time_hi_and_version",
        // four most significant bits holds version number 4
        mt_rand(0, 0x0fff) | 0x4000,

        // 16 bits, 8 bits for "clk_seq_hi_res",
        // 8 bits for "clk_seq_low",
        // two most significant bits holds zero and one for variant DCE1.1
        mt_rand(0, 0x3fff) | 0x8000,

        // 48 bits for "node"
        mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
    );

}

function is_uuid($uuid) {
    return is_string($uuid) && preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/', $uuid) === 1;
}

function is_valid_username($username) {
    return strlen($username) > 2 && strlen($username) <= 36;
}

function is_valid_password($password) {
    // password must be at least 8 characters long
    // and contain at least one number and one lowercase and one uppercase letter
    // do not allow whitespace
    return strlen($password) >= 8 &&
        preg_match('/[a-z]/', $password) &&
        preg_match('/[A-Z]/', $password) &&
        preg_match('/[0-9]/', $password) &&
        !preg_match('/\s/', $password);
}
