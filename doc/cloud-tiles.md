# Cloud Tiles

Cloud tiles are an advanced feature that allows you to seamlessly synchronize your tiles between multiple different
devices. This requires you to own a SQL database and a webserver that you can execute PHP on.

1. use the **[Initialization SQL file](../launch-anything-cloud/database/init.sql)** to create the database and tables.
2. in the **[Database Connection Script](../launch-anything-cloud/php/connect_database.php)**, replace the empty strings
   with your database credentials.  
  (example: `$db = new mysqli('localhost', 'fd465de', 'fve65utwzh9r1u', 'fd465de');`)
3. upload the **[Cloud Tiles Scripts](../launch-anything-cloud/php)** to your webserver.
4. in the launch-anything settings, use the `Configure API URL` button in the `Cloud Tiles` section to configure the API
   URL. This is the URL to the directory your PHP files are located in.
5. create an account in the `Cloud Tiles` section using a username and password.

There you go, cloud tiles that you now create are being uploaded to your database.
