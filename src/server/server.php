<?php
// finds out if given session name is in the list of sessions
function is_in_sessions($session_name)
{
    $handle = fopen("sessions.txt", "a+");
    if ($handle) {
        while (($line = fgets($handle)) !== false) {
            if (explode(" t: ", $line)[0] === "s: " . $session_name)
                return true;
        }
        fclose($handle);
    }
    return false;
}
// adds session name into list of sessions
function add_to_sessions($session_name)
{
    file_put_contents("sessions.txt", "s: " . $session_name . " t: " . date('d-m-Y H:i:s') . " a: A", FILE_APPEND | LOCK_EX);
}
// replaces a single line
function replace_a_line($data, $session_name, $timestamp)
{
    if ($timestamp === false)
        $state = " a: Q";
    else
        $state = " a: A";
    if (strpos($data, 's: ' . $session_name) === 0) {
        return "s: " . $session_name . " t: " . date('d-m-Y H:i:s') . $state . "\n";
    }
    return $data;
}
// updates either last update timestamp or status
function update_state($session_name, $timestamp)
{
    $data = file('sessions.txt');
    $data = array_map(function($data) use ($session_name, $timestamp)
    {
        return replace_a_line($data, $session_name, $timestamp);
    }, $data);
    file_put_contents('sessions.txt', implode('', $data));
}
// save recording data
function save_data($session_name, $recording)
{
    $recording = str_replace(' ', '+', $recording);
    $recording = base64_decode($recording);
    $response  = array();
    $fp        = fopen('recordings/' . $session_name . '/recording.raw', 'a+b');
    fwrite($fp, $recording);
    fclose($fp);
}
if (preg_match('/\.(?:php|raw|log)/', $_SERVER["REQUEST_URI"])) {
    return false;
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $type = $_POST['type'];
    if ($type == "log") {
        $session          = $_POST['session'];
        // is it log from previous session
        $previous_session = $_POST['previous_session'];
        if ($previous_session == "true") {
            if (!is_in_sessions($session))
                add_to_sessions($session);
            update_state($session, false);
            if (!file_exists("recordings/" . $session))
                mkdir("recordings/" . $session, 0777, true);
        } else {
            update_state($session, true);
        }
        $log_record = $_POST['message'];
        $timestamp  = $_POST['time'];
        $elapsed    = $_POST['elapsed'];
        $record     = $timestamp . " - " . $session . " - " . $elapsed . "s - " . $log_record;
        file_put_contents('recordings/' . $session . '/server.log', $record . PHP_EOL, FILE_APPEND | LOCK_EX);
        echo "OK";
    } else if ($type == "record") {
        $session          = $_POST['session'];
        // if it isnt from previous session, save also last packet
        $previous_session = $_POST['previous_session'];
        $recording        = $_POST['data'];
        if ($previous_session == "false") {
            update_state($session, true);
            $recording_dec = str_replace(' ', '+', $recording);
            $recording_dec = base64_decode($recording_dec);
            $fp            = fopen('recordings/' . $session . '/last_packet.raw', 'wb');
            fwrite($fp, $recording_dec);
            fclose($fp);
        }
        save_data($session, $recording);
        if (intval($_POST['bytes']) == strlen($_POST['data']))
            echo "OK";
        else
            echo "ERROR";
    } else if ($type == "previousRecord") {
        $session = $_POST['session'];
        if (!is_in_sessions($session))
            add_to_sessions($session);
        update_state($session, false);
        if (!file_exists($session))
            mkdir("recordings/" . $session, 0777, true);
        $recording = $_POST['data'];
        save_data($session, $recording);
        if (intval($_POST['bytes']) == strlen($_POST['data']))
            echo "OK";
        else
            echo "ERROR";
    } else if ($type == "handshake") {
        $session = $_POST['session'];
        if (!file_exists("recordings/" . $session)) {
            mkdir("recordings/" . $session, 0777, true);
        }
        add_to_sessions($session);
        echo "OK";
    } else if ($type == "goodbye") {
        $session = $_POST['session'];
        $log     = $_POST['data'];
        file_put_contents('recordings/' . $session . '/client.log', $log . PHP_EOL, FILE_APPEND | LOCK_EX);
        update_state($session, false);
        echo "BYE";
        exit();
    }
}
?>
