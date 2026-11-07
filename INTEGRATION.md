# Chicken Monitor integration

## Run locally

Start the Spring Boot app from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`. Stop the server with `Ctrl+C` in that terminal.

Flyway baselines an existing schema at version 1 and applies `V2__add_telemetry_controls.sql`. Back up MySQL before the first run against a database containing important data. Public registration creates `USER` accounts only. Bootstrap the first administrator directly in MySQL after confirming the username:

```sql
UPDATE `user` SET role = 'ADMIN' WHERE username = 'your-admin-username';
```

## MQTT

MQTT is disabled unless `MQTT_ENABLED=true`. Configure these environment variables before starting the app:

- `MQTT_ENABLED=true`
- `MQTT_BROKER_URL=tcp://localhost:1883`
- `MQTT_CLIENT_ID=chickenmonitor-server`
- `MQTT_USERNAME` and `MQTT_PASSWORD` when the broker requires authentication

The server subscribes to:

- `chickenmonitor/devices/+/register` for the first device inventory announcement
- `chickenmonitor/devices/+/telemetry` for readings and actor state acknowledgements

The device code in the topic must match `idDevice` in the JSON body. A device code must already exist in admin provisioning. Unknown or unassigned codes are ignored.

Example registration/telemetry payload:

```json
{
  "idDevice": "stm32_2",
  "actors": [
    { "idActor": "pump2", "typeActor": "pump", "mode": 1, "status": 0 },
    { "idActor": "fan2", "typeActor": "fan", "mode": 0, "status": 1 }
  ],
  "sensors": [
    { "idSensor": "temp_dht11_2", "value": 29.0, "unit": "C" },
    { "idSensor": "nh3_2", "value": 4.1, "unit": "ppm" }
  ]
}
```

The `register` message upserts sensor and actor hardware codes under that provisioned device. Later `telemetry` messages append sensor readings and update actor states. Sensor `idSensor` and actor `idActor` must match the hardware codes configured during provisioning; `typeSensor`/`typeActor` are display/category values.

The server publishes commands to `chickenmonitor/devices/{deviceCode}/commands`:

```json
{ "idDevice": "stm32_2", "idActor": "pump2", "mode": 1, "status": 1 }
```

`mode=0` means automatic and `mode=1` means manual. `status=0` means off and `status=1` means on. Manual commands are accepted only in manual mode. For automatic actors, configure a sensor code and `thresholdMin < thresholdMax`; at or above the maximum the server requests `status=1`, at or below the minimum it requests `status=0`, and between them it keeps the prior device state. The dashboard reflects the device's reported status, not merely a published command.

Live browser updates use authenticated WebSocket `/ws/live`, scoped to the logged-in username. They are emitted after MQTT messages have been stored.

## Cameras

The dashboard requests a stream only after a user clicks a configured camera. Configure `CAMERA_SERVICE_URL` and, when needed, `CAMERA_SERVICE_TOKEN`. The backend sends this request to `{CAMERA_SERVICE_URL}/api/streams`:

```json
{ "deviceCode": "camera_house_1" }
```

The camera service must return JSON containing `streamUrl` and optionally `expiresAt`, for example `{"streamUrl":"https://camera.example/live/house-1/index.m3u8","expiresAt":"2026-10-28T15:00:00Z"}`. The current browser player supports HLS (through hls.js) and browser-native video URLs such as MP4. It does not expose RTSP credentials or raw RTSP URLs to the browser. WebRTC signaling is not implemented yet.

## Current API behavior

- `GET /api/farms` returns the authenticated user's farms; create/update/delete are owner-scoped.
- `GET /api/dashboard?farmId={id}` returns devices, latest sensor samples, and actors for an owned farm.
- `GET /api/sensors/{sensorId}/data?period=day|week|month` returns up to 500 raw samples for that window.
- `GET/POST/PUT/DELETE /api/schedules` is owner-scoped; schedules run daily at the configured local time.
- `/api/admin/devices/provision` is ADMIN-only and can attach a device to a user/farm or create an unassigned code for a user to claim.
- `/api/users/**` is ADMIN-only and returns user DTOs without password hashes.
