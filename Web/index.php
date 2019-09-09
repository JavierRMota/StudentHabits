<?php
$host = 'HOST';
$user = 'USER';
$passwd = 'PSSWD';
$db = 'DB';
$connection = new mysqli($host, $user, $passwd,$db);
if (!$connection){
    die("Database Connection Failed" . mysqli_error($connection));
}
$path = rtrim($_SERVER['REQUEST_URI'], '/');     // Trim leading slash(es)
$path = substr($path, strpos($path, "API")+4);
$elements = explode('/', $path);                // Split path on slashes
if(empty($elements[0])) {                      // No path elements means home
} else switch(array_shift($elements))             // Pop off first item and switch
{
    case 'NEW':
        newDevice();
        break;
    case 'DATA':
        newData();
        break;
    default:
        header('HTTP/1.1 404 Not Found');
        echo "404";
}
function newDevice() {
  global $connection;
  $id = md5(time());
  $queryDev = $connection->prepare("INSERT INTO IDENTIFIER(IDENTIFIER) VALUES (?)");
  $queryDev->bind_param("s", $id);
  if($queryDev->execute()){
    $data = array("id" => $id);
  } else {
    $data = array("id" => "");
  }
  header('Content-Type: application/json');
  echo json_encode($data);
}
function newData() {
  // Takes raw data from the request
  $json = file_get_contents('php://input');
  // Converts it into a PHP object
  $data = json_decode($json,true);
  global $connection;
  header('Content-Type: application/json');
  $queryID = $connection->prepare("SELECT * FROM IDENTIFIER WHERE IDENTIFIER = ?");
  $queryID->bind_param("s", $data["id"]);
  $queryID->execute();
  $result = $queryID->get_result();
  $errorCount = 0;
$response = array();
  if ($result->num_rows == 1) {
    $row = $result->fetch_assoc();
    $id = $row["ID"];
    $dataInsert = $connection->prepare("INSERT INTO DATA(ID,NAME,PKG_NAME,START_TIME,END_TIME,DATE_EVENT,DURATION) VALUES (?,?,?,?,?,?,?)");
    $arrayData = json_decode($data["data"],true);
    foreach ($arrayData as $_ => $activity) {
      $dataInsert->bind_param("issiisi",$id, $activity["name"], $activity["pkg_name"], $activity["start"], $activity["end"], $activity["date"], $activity["duration"]);
      if(!$dataInsert->execute() && $dataInsert->errno != 1062) {
	$response["errorType"+$errorCount] = $dataInsert->error ;
        $errorCount++;
      }
    }
    if($errorCount >  0) {
      $response["error"] = true;
        $response["count"] = $errorCount;
    } else {
      $response["error"] = false;
    }
  } else {
	$response["error"] = true;
	$response["count"] = $errorCount;
  }
echo json_encode($response);

}
