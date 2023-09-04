## PolyBot settings guide

PolyBot features a list of avaliable settings that can be changed on the fly by utilizing the `settings` command.
This command is locked to members who have the permission to kick server members, and bot owners.
A list of available settings can be obtained by running `&settings list`, this provides a list of every bot setting that can be changed, and general information about whether it stores multiple or just one value.
The settings system allows paraphrasing some words used for the setting name. (eg. "level" with "lvl", "message" with "msg", "background" with "bg", etc)

There are three types of settings.
- Non appendable with placeholders
- Non appendable
  - These settings only allow one value.
  - If the setting supports placeholders, a placeholder can be provided by specifying `%s` in the value. Placeholders are optional.
- Appendable
  - These settings support multiple values, split up with a `,`.
  - Removing a setting requires an exact match of the value you wish to remove.
  - Changing these settings will by default add a new value.
  - 
*Note: For the examples below, be sure to replace values with `()`, `[]` with their actual value.*


### Retrieving a setting
`&settings (setting)`
This will return the value currently stored for this setting. By default, an unset or cleared setting will return `null` or ` `.

### Changing a setting
`&settings (setting), (value)`

Depending on the setting provided being appendable, it will either add to the existing value, or set it as the new value.

### Removing a value from a setting
`&settings (setting), remove (value)`

If the value provided does not exist, the setting will be left unchanged. If it does, it will be removed and adjust the other values accordingly.

### Clearing/resetting a setting
`&settings (setting), clear`

This will delete whatever value is set in the setting. Cleared settings will return `null`.

### Examples

- `&settings user blacklist, 567977785893847050`
  - Blacklists `567977785893847050` from using the bot.
- `&settings user blacklist, 805687770818936845|media`
  - Blacklists `805687770818936845` from using any command listed in the "media" category.
- `&settings user blacklist, remove 567977785893847050`
  - Removes `567977785893847050` from the blacklist.
- `&settings channel blacklist, 834437126203768852`
  - Channel blacklist works the same way as the three examples above, but instead of a user id, you provide a channel id.
  
- `&settings lvl up msg, %s has leveled up to %s!`
  - Sets the level up message.
  - "PolyBot has leveled up to 4!"
- `!!settings dyno log channel, 804493720862457886`
  - Log warns and kicks issued by the `&warning` commands to the channel id provided above.
