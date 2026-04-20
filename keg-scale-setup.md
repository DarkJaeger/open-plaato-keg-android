# Keg Scale Setup Guide

This guide walks through connecting, configuring, and calibrating a keg scale in the Open Plaato Keg app.

---

## Prerequisites

- A running [Plaato server](https://github.com/danielfalk/plaato-keg) on your local network
- Your Plaato Keg scale already registered on the server
- The Open Plaato Keg Android app installed

---

## Step 1: Connect to Your Server

1. Open the app and tap the **Settings** tab (bottom navigation bar).
2. Enter your server URL in the format `http://<ip-address>:<port>` (e.g., `http://192.168.1.10:8085`).
3. Tap **Connect**. The app will verify the server and establish a WebSocket connection.

> Once connected, your keg scales will appear automatically in the **Scales** tab. No manual pairing or discovery is required.

---

## Step 2: Open Scale Configuration

1. Tap the **Scales** tab.
2. Tap on a scale card to open its configuration screen.

---

## Step 3: Configure Display Settings

### Units
- **Metric / US** — Choose whether amounts are displayed in litres/kilograms or gallons/pounds.
- **Weight / Volume** — Choose whether the remaining beer is shown as a weight or a volume.

### Keg Mode
- **Beer** — Standard mode for beer kegs.
- **CO₂** — Use this mode if the scale is monitoring a CO₂ cylinder.

---

## Step 4: Set Pour Sensitivity

Adjust how sensitive the scale is to detecting an active pour. Higher sensitivity detects smaller flow rates.

| Setting   | Best for                                  |
|-----------|-------------------------------------------|
| Very Low  | High-vibration environments               |
| Low       | General use with minimal interference     |
| Medium    | Standard setups (recommended starting point) |
| High      | Low-flow taps or very quiet environments  |

---

## Step 5: Calibrate the Scale

Calibration tells the app how much liquid is in the keg. There are three inputs:

### Empty Keg Weight
Enter the weight of your empty keg (in kg). This is used to subtract the keg tare weight when calculating how much beer remains.

- Common values: Cornelius (Ball Lock) ≈ 4.0 kg, Cornelius (Pin Lock) ≈ 4.1 kg, Euro-style Sankey ≈ 9.0–12.0 kg

### Max Keg Volume
Enter the full capacity of the keg in litres (e.g., `19.0` for a half-barrel Corny keg). This is used to calculate the percentage of beer remaining.

### Calibrate with Known Weight *(optional)*
If the scale reading seems inaccurate, place a known weight on the scale and enter its value here. This corrects the hardware calibration offset.

### Temperature Offset *(optional)*
If the displayed temperature does not match a reference thermometer, enter a positive or negative offset (°C) to compensate.

---

## Step 6: Tare the Scale

Taring zeroes out the scale with an empty keg on it, so only the liquid weight is measured.

1. Place an **empty keg** on the scale.
2. Tap **Tare Scale** and hold the button for **3 seconds**.
3. Release when prompted. The scale will zero itself.

> Tare whenever you swap to a different style or size of keg.

---

## Step 7: Set Empty Keg (Hardware Calibration)

This sends a calibration command directly to the scale hardware.

1. Place your **empty keg** on the scale.
2. Tap **Set Empty Keg** and hold for **3 seconds**.
3. Release when prompted.

> Use this after taring, or any time the scale hardware needs to be recalibrated from scratch.

---

## Step 8: Reset Last Pour *(optional)*

If the last pour reading is stale or incorrect, tap **Reset Last Pour** to clear it.

---

## Live Readings

Once configured, the scale card and detail screen display live data updated in real time via WebSocket:

| Reading           | Description                              |
|-------------------|------------------------------------------|
| Amount Left       | Remaining beer (in your chosen unit)     |
| % Remaining       | Percentage of the original fill left     |
| Temperature       | Keg temperature (°C or °F)              |
| Raw Weight        | Raw scale reading in kg                  |
| Pouring           | Indicates if a pour is currently active  |
| Last Pour         | Volume of the most recent pour           |

---

## Troubleshooting

**Scales tab is empty**
- Check the server URL in Settings and reconnect.
- Confirm the scale is registered and online on the server.

**Weight reads incorrectly after keg swap**
- Re-enter the correct **Empty Keg Weight** for the new keg, then **Tare** with the empty keg on the scale.

**Pour not detected**
- Increase the **Pour Sensitivity** setting.

**Temperature is off**
- Use the **Temperature Offset** field to dial in the reading.

**Scale shows stale data**
- Check the WebSocket connection in Settings. Disconnect and reconnect if needed.
