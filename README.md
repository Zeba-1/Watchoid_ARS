# Watchoid

## Project Overview

This project aims to develop an Android application capable of monitoring network services and notifying users in case of detected anomalies. All detected events will be logged and accessible for later consultation

## Features

### Supported Network Services

The application will support monitoring various network services, including:

- UDP services

- TCP services

- HTTP-based services (e.g., web servers)

- ICMP (ping) accessibility

### Service Testing Mechanism

The application will send predefined requests to network services and compare the responses to expected results to determine their status (success or failure). Examples include:

- Checking the presence of specific content in an HTTP response

- Using regular expressions to validate textual data

Tests will only run when network connectivity is available and can be restricted based on conditions such as:

- WiFi vs. cellular network

- Battery level (e.g., tests only run when charging)

### Logging and Data Storage

All test results (success or failure) will be logged.

Metrics such as execution time, latency, and response statistics will be recorded.

A graphical interface will allow users to view test history.

Logs will be configurable with a retention policy and can be exported/imported in an encrypted format.

### Easter Egg: Mini-Game

Each developer will implement a unique mini-game as a hidden easter egg in the application, activated through a secret action (to be revealed during the final presentation).
