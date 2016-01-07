<!DOCTYPE html>
<html>
<head>
  <title>Internet of Things Foundation Historian</title>
</head>
<body>
  <table>
  % for item in env_options:
    <tr><td>{{item}}</td><td>{{env_options[item]}}</td></tr>
  % end
  </table>
</body>
</html>