# PolyBot
A bot for New Polynomers Country providing novelty and utility commands.

## How to run
Not much effort has been made to make sure the bot can start up just fine in a fresh enviroment, but most features that cannot enable will not be enabled. (Rendering, fonts, leveling, user and bot settings)

## bot.properties
To run the bot, you need to create a file called `bot.properties` and customize the following values:

| Key  | Value |
| ------------- | ------------- |
| `discord-bot-token` | The bot token to use when connecting to discord |
| `debugging` | Toggles debug logging in the console |
| `cat-api-key` | API key to use for a random cat image using `&cat` |
| `dog-api-key` | API key to use for a random dog image using `&dog` |
| `renderers` | List of PolyBot rendering services to attempt to use for media commands. |

A renderer can be provided with the following format:

`(server ip),(enabled),(server port),(web port)`

`127.0.0.1,true,5678,8080`

Multiple renderers can be specified by splitting them up using a `|`.


