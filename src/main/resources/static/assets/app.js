// Dashboard entry point. Feature code lives in /assets/app/.
import { setPage, bindNavigationEvents } from "./app/navigation.js";
import { bindFarmEvents } from "./app/farms.js";
import { bindDeviceEvents } from "./app/devices.js";
import { bindActorEvents } from "./app/actors.js";
import { bindScheduleEvents } from "./app/schedules.js";
import { bindNotificationEvents } from "./app/notifications.js";
import { bindCameraEvents } from "./app/cameras.js";
import { bindHistoryEvents } from "./app/history.js";
import { bindSessionEvents, startApp } from "./app/session.js";

// Register listeners before fetching data so the UI remains responsive.
bindNavigationEvents();
bindFarmEvents(setPage);
bindDeviceEvents();
bindActorEvents();
bindScheduleEvents();
bindNotificationEvents();
bindCameraEvents();
bindHistoryEvents();
bindSessionEvents();

startApp();
