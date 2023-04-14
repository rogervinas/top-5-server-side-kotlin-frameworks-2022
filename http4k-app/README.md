# Http4k

Project Wizard https://toolbox.http4k.org/project
* Server 
* Do you need server-side WebSockets or SSE? No 
* Select a server engine: Undertow 
* Select HTTP client library: OkHttp
* Select JSON serialisation library: Jackson
* Select a templating library: None
* Select any other messaging formats used by the app: None
* Select any integrations that catch your eye! None
* Select any testing technologies to battle harden the app:
* Application identity:
  * main class: GreetingApplication
  * base package: org.rogervinas
* Select a build tool: gradle
* Select a packaging type: ShadowJar

https://toolbox.http4k.org/stack/dD1BQU1BWlFES0FTOEI5Z1BvQS1rRXNBVVcmYz1HcmVldGluZ0FwcGxpY2F0aW9uJnA9b3JnLnJvZ2VydmluYXM

No suporta kotlin gradle?
ojo que no genera .gitignore
java 11 

./gradlew run 

curl http://localhost:9000/ping
pong

curl http://localhost:9000/formats/json/jackson
{"subject":"Barry","message":"Hello there!"}

https://www.http4k.org/guide/tutorials/tdding_http4k/
https://www.http4k.org/guide/howto/write_different_test_types/


CLI or intelliJ plugin https://toolbox.http4k.org/
