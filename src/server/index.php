<?php

// removes session name from list of sessions
function remove_from_sessions($session_name) {
        $sample = 's: ' . $session_name;
	$sessions = "";
        $handle = fopen('sessions.txt','r');
	while (($line = fgets($handle)) !== false) {
                if (strpos($line, $sample) !== 0) {
                        $sessions = $sessions . $line;
                }
        }
        fclose($handle);
	file_put_contents('sessions.txt', $sessions);
}

    if($_SERVER['REQUEST_METHOD'] == "GET" and isset($_GET['delete']))
    {
        remove_from_sessions($_GET['delete']);
    }

?>

<h1>
	<center> Audio Recorder Sessions</center>
</h1>
<center><table cellspacing="20"  style='border: 1px solid #000;'>
	<tr>
		<th>Session name</th>
		<th>Last packet time</th>
		<th>Status</th>
		<th>Last packet</th>
		<th>Server log</th>
		<th>Client log</th>
		<th>Delete</th>
	</tr>
		<?php
			$handle = fopen("sessions.txt", "r");
			if ($handle) {
			while (($line = fgets($handle)) !== false) {
				$line = trim($line);
				$line = explode("a: ", $line);
				$status = $line[1];
				$line = explode("t: ", $line[0]);
				$timestamp = $line[1];
				$session = explode("s: ", $line[0])[1];
				$last = new \DateTime($timestamp);
				$current = new \DateTime();
				$interval = $current->diff($last);
				$minutes = $interval->days * 24 * 60;
				$minutes += $interval->h * 60;
				$minutes += $interval->i;
				echo "<tr>";
					echo "<td>". $session . "</td>";
				echo "<td>" . $timestamp . "</td>";
				if ($status === "Q") echo "<td style='color: black;'> ENDED (" . $minutes . " minutes ago)";
				else if ($minutes < 5) {
					echo "<td style='color: green;'> ACTIVE (" . $minutes . " minutes ago)";
				} else {
					echo "<td style='color: red;'> INACTIVE (" . $minutes . " minutes ago)";
				}
				if (file_exists("recordings/" . trim($session) . "/last_packet.raw")) {
					echo "<td><a href='/recordings/" . trim($session) . "/last_packet.raw'>download</a></td>";
				} else {
					echo "<td>Unavailable</td>";
				}	
				echo "<td><a href='/recordings/" . trim($session) . "/server.log'>download</a></td>";
				if (file_exists("recordings/" . trim($session) . "/client.log")) {
					echo "<td><a href='/recordings/" . trim($session) . "/client.log'>download</a></td>";
				} else {
					echo "<td>Unavailable</td>";
				}
				echo "<td> <form action='' method='GET' style='margin: 0;'> <button type='submit' value='" . $session ."' name='delete'>X</button> </form> </td>";
				echo "</tr>";
		}
		fclose($handle);
		} else {
		echo "Error opening file";}
		?>
</table></center>
