# VelocityPartyManager

**VelocityPartyManager** is a Velocity plugin written in Kotlin that provides a robust party management system for your
proxy network. With a built-in REST API, it allows external applications and services to interact with the
plugin—registering, joining, leaving, and managing parties seamlessly.

## Features

- **Party Creation & Registration:** Easily register a new party with a designated leader.
- **Join & Leave Parties:** Allow players to join or leave existing parties.
- **Party Management:** Unregister a party, transfer party leadership, or initiate a party transfer.
- **REST API:** Built-in REST endpoints (powered by Javalin) to interact with party functionality programmatically.
- **Mockable for Testing:** Uses dependency injection to simplify testing and integration with Velocity.

## Requirements

- **Velocity Proxy:** Compatible with Velocity 3.3.0-SNAPSHOT (or later).
- **Java:** Java 17 or higher.
- **Gradle:** For building the plugin.
- **Kotlin:** Project is written in Kotlin.

## Installation

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/yourusername/VelocityPartyManager.git
   cd VelocityPartyManager
   ```

2. **Build the Plugin:**

   Use Gradle to build the jar file. For example:

   ```bash
   ./gradlew shadowJar
   ```

   This will produce a jar file (e.g., `velocityparty-manager-all.jar`) in the `build/libs` directory.

3. **Deploy to Velocity:**

   Copy the generated jar file to your Velocity `plugins` folder.

4. **Restart Velocity:**

   Restart your Velocity proxy server to load the plugin.

## Configuration

By default, the REST API port is hard-coded (e.g., port `7000`) when initializing the server. You can update the port or
other configuration settings by editing the configuration file (if provided) or through environment variables. *(Coming
Soon: A configuration file to customize endpoints and port.)*

## REST API Endpoints

The plugin exposes several endpoints for party management. Here are some examples:

- **Register Party**

    - **Endpoint:** `POST /party/register`
    - **Query Parameter:** `leaderUuid` (UUID of the player creating the party)
    - **Success Response:** `201 Created` with JSON containing the `partyUUID`.

- **Join Party**

    - **Endpoint:** `POST /party/join`
    - **Query Parameters:** `partyUUID` and `playerUUID`
    - **Success Response:** `200 OK` with a message `"Joined party successfully"`.

- **Leave Party**

    - **Endpoint:** `POST /party/leave`
    - **Query Parameter:** `playerUUID`
    - **Success Response:** `200 OK` with a message `"Left party successfully"`.

- **Unregister Party**

    - **Endpoint:** `POST /party/unregister`
    - **Query Parameter:** `playerUUID`
    - **Success Response:** `200 OK` with a message `"Party unregistered successfully"`.

- **Transfer Party or Leader**

    - **Endpoint:** `POST /party/transfer` and `POST /party/transferLeader`
    - **Query Parameters:** Vary depending on the operation (e.g., `playerUUID`, `serverAlias`, `newLeaderUUID`).

- **Get Party Info**

    - **Endpoint:** `GET /party/info`
    - **Query Parameter:** `playerUUID`
    - **Success Response:** `200 OK` with party details in JSON format.

## Running Tests

The project includes comprehensive integration tests for the REST API endpoints. To run the tests:

```bash
./gradlew test
```

The tests use OkHttp to perform HTTP requests against the server running on dynamically assigned ports, ensuring that
tests do not conflict with each other.

## Contributing

Contributions are welcome! If you have suggestions, bug fixes, or new features, please open an issue or submit a pull
request.

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and open a pull request.

## License

This project is licensed under the [AGPLv3 License](LICENSE).

## Support

If you encounter issues or have questions, please open an issue on
the [GitHub issue tracker](https://github.com/yourusername/VelocityPartyManager/issues).

---

*Happy Party Managing!*