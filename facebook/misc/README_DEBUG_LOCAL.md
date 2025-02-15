# Test local app

It is beneficial to test your app locally, before publishing it.
Here we describe how to do that.

## Facebook

### Create a FB Application

* Go to [developers.facebook.com](http://developers.facebook.com) and create an application.

* Make note of the `Application ID`

### Settings

* Under `Settings` -> `Basic` -> `App Domains`, set the value `localhost`

* At the bottom of the page, press `Add Platform` and add `Facebook Web Games`

* Under `Facebook Web Games` -> `Facebook Web Games URL (https)` set

		https://localhost:8000/index.html?

### Facebook Login

* For the application, under `Products` add the `Facebook Login` application.

* Under `Client OAuth Settings` -> `Valid OAuth Redirect URIs` set the value:

		https://localhost:8000


## Bundle your game

* Add the `Application ID` to your game.project:

		[facebook]
		appid = YOUR_APPLICATION_ID

* Bundle the game to HTML5

## HTTPS server

* Open a terminal/console window in your bundled HTML5 game folder (where `index.html` is):

		$ cd path_to_my_game/

* Make sure you have the script `misc/facebook_server.py` available locally

* A `key.pem` and `cert.pem` are required for the next step. You can either run [create_cert.sh](./facebook/misc/create_cert.sh) or run:

		openssl req -x509 -newkey rsa:2048 -days 30 -subj "/CN=Test Team" -nodes -keyout "key.pem" -out "cert.pem" >
		/dev/null 2>&1


* Launch the http server:

		$ python ~/facebook_server.py 8000

Or all at once, assuming the bundle-dir is directly under the `extension-facebook` folder:

		$ (cd bundle-dir/extension-facebook/ && ../../facebook/misc/create_cert.sh && python ../../facebook/misc/facebook_server.py 8000)

## Test the app

* Go to your app page: https://apps.facebook.com/<APP_ID>
