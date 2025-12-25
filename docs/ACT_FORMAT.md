# ACT Format Documentation

## Overview

ACT (Availability-Cycle-Timing) is a flexible scheduling format that controls when treasure hunt collections are available to players. Each collection can have multiple ACT rules, and the collection becomes available when **any enabled rule is active** (OR logic).

## Format Structure

```
[DATE_RANGE] [DURATION] [CRON_EXPRESSION]
```

All three components are required and must be enclosed in square brackets.

---

## Components

### 1. DATE_RANGE

Defines the date window during which the rule can be active.

**Formats:**
- `*` - Always valid (no date restriction)
- `YYYY-MM-DD:YYYY-MM-DD` - Specific date range (ISO 8601 format)

**Examples:**
```
[*]                           # Valid any date
[2025-12-01:2025-12-31]      # Valid only in December 2025
[2025-06-15:2025-06-15]      # Valid only on June 15, 2025
[2024-01-01:2026-12-31]      # Valid for 3 years
```

**Notes:**
- Start and end dates are inclusive
- Times are evaluated in the server's timezone
- If current date is outside the range, the rule is inactive regardless of other components

---

### 2. DURATION

Specifies how long the collection remains available once triggered.

**Formats:**
- `*` - Always active (no time limit)
- `<number><unit>` - Specific duration

**Units:**
- `s` - Seconds
- `m` - Minutes
- `h` - Hours
- `d` - Days

**Examples:**
```
[*]          # Always active (no time limit)
[30m]        # Active for 30 minutes
[2h]         # Active for 2 hours
[8h]         # Active for 8 hours
[24h]        # Active for 24 hours (1 day)
[7d]         # Active for 7 days
[90s]        # Active for 90 seconds
```

**Notes:**
- Duration starts when the cron expression triggers
- For `MANUAL` triggers, duration starts when admin activates the rule
- Once duration expires, the rule becomes inactive until next trigger

---

### 3. CRON_EXPRESSION

Determines when the rule activates (triggers the duration countdown).

**Formats:**
- `NONE` - Never triggers automatically (rule stays inactive)
- `MANUAL` - Only activates via admin command (no auto-trigger)
- Quartz Cron Expression - Standard 6 or 7 field format

**Quartz Cron Format:**
```
┌─── second (0-59)
│ ┌─── minute (0-59)
│ │ ┌─── hour (0-23)
│ │ │ ┌─── day of month (1-31)
│ │ │ │ ┌─── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌─── day of week (0-6 or SUN-SAT, 0=Sunday)
│ │ │ │ │ │ ┌─── year (optional, 1970-2099)
│ │ │ │ │ │ │
* * * * * * *
```

**Special Characters:**
- `*` - All values (every)
- `?` - No specific value (used for day-of-month or day-of-week)
- `-` - Range (e.g., `1-5`)
- `,` - List (e.g., `1,3,5`)
- `/` - Increments (e.g., `*/15` = every 15)
- `L` - Last (e.g., `L` in day-of-month = last day of month)
- `W` - Weekday (e.g., `15W` = weekday nearest to 15th)
- `#` - Nth day (e.g., `2#1` = first Monday of month)

**Examples:**
```
[NONE]                    # Never activates
[MANUAL]                  # Admin-triggered only
[0 0 9 * * ?]            # Every day at 9:00 AM
[0 0 0 25 12 ?]          # Christmas Day at midnight
[0 0 12 ? * MON-FRI]     # Weekdays at noon
[0 */30 * * * ?]         # Every 30 minutes
[0 0 8,20 * * ?]         # Every day at 8 AM and 8 PM
[0 0 0 1 * ?]            # First day of every month at midnight
[0 0 14 ? * SAT,SUN]     # Weekends at 2 PM
```

---

## Complete Examples

### Seasonal Events

**Halloween Event (8 hours on October 31st)**
```
[2025-10-31:2025-10-31] [8h] [0 0 18 31 10 ?]
```
Activates at 6:00 PM on Halloween, available for 8 hours (until 2:00 AM).

**Christmas Week (24 hours daily)**
```
[2025-12-20:2025-12-26] [24h] [0 0 0 * * ?]
```
Activates at midnight every day during Christmas week, available all day.

**Summer Event (Entire June)**
```
[2025-06-01:2025-06-30] [*] [NONE]
```
Available 24/7 throughout June, no daily triggers needed.

---

### Daily Events

**Daily Morning Hunt (2 hours)**
```
[*] [2h] [0 0 8 * * ?]
```
Every day at 8:00 AM, available for 2 hours.

**Weekend Afternoon Hunt**
```
[*] [4h] [0 0 14 ? * SAT,SUN]
```
Saturdays and Sundays at 2:00 PM, available for 4 hours.

**Weekday Evening Hunt**
```
[*] [3h] [0 0 18 ? * MON-FRI]
```
Weekdays at 6:00 PM, available for 3 hours.

---

### Weekly Events

**Weekly Reset (Every Monday)**
```
[*] [7d] [0 0 0 ? * MON]
```
Activates Monday at midnight, available for entire week.

**First Weekend of Month**
```
[*] [48h] [0 0 0 ? * SAT#1]
```
First Saturday of each month, available for 48 hours.

---

### Special Patterns

**Hourly Mini-Events (30 minute windows)**
```
[*] [30m] [0 0 * * * ?]
```
Triggers every hour on the hour, available for 30 minutes.

**Night Time Only**
```
[*] [8h] [0 0 22 * * ?]
```
Every night at 10:00 PM, available for 8 hours (until 6:00 AM).

**Manual Event (Admin controlled)**
```
[2025-01-01:2025-12-31] [6h] [MANUAL]
```
Only activates when admin manually triggers it, runs for 6 hours.

**Always Available**
```
[*] [*] [NONE]
```
Permanently available, no restrictions.

---

## Multiple Rules

Collections can have multiple ACT rules. The collection is available when **ANY enabled rule is active** (OR logic).

**Example: Weekend + Holiday Hunt**
```
Rule 1: [*] [48h] [0 0 0 ? * SAT]           # Every weekend
Rule 2: [2025-12-25:2025-12-25] [24h] [0 0 0 25 12 ?]  # Christmas
```
This collection is available every weekend AND on Christmas Day.

**Example: Morning + Evening Availability**
```
Rule 1: [*] [3h] [0 0 8 * * ?]    # Morning: 8 AM - 11 AM
Rule 2: [*] [3h] [0 0 18 * * ?]   # Evening: 6 PM - 9 PM
```
Collection is available twice daily.

**Example: Phased Event**
```
Rule 1: [2025-01-01:2025-03-31] [*] [NONE]   # Q1: Always available
Rule 2: [2025-04-01:2025-06-30] [12h] [0 0 8 * * ?]  # Q2: Morning only
Rule 3: [2025-07-01:2025-09-30] [24h] [0 0 0 ? * SAT,SUN]  # Q3: Weekends only
```
Availability changes throughout the year.

---

## Best Practices

### 1. **Use Meaningful Names**
Give each rule a descriptive name in the editor:
- "Weekend Morning Hunt"
- "Christmas Special"
- "Weekday Evening Event"

### 2. **Set Priorities**
Higher priority rules take precedence for display purposes:
- 100+ - Critical events (holidays)
- 50-99 - Special events
- 1-49 - Regular schedules
- 0 - Default

### 3. **Test Your Cron Expressions**
Use online cron validators to test expressions:
- [Cron Expression Generator](https://www.freeformatter.com/cron-expression-generator-quartz.html)
- Verify next trigger times before deploying

### 4. **Consider Timezones**
All times use the server's timezone. Communicate this to players.

### 5. **Avoid Overlapping Durations**
If rules overlap, the collection stays available (OR logic). This is usually fine, but be aware:
```
Rule 1: [*] [6h] [0 0 8 * * ?]   # 8 AM - 2 PM
Rule 2: [*] [6h] [0 0 12 * * ?]  # 12 PM - 6 PM
# Collection is actually available 8 AM - 6 PM (10 hours)
```

### 6. **Use MANUAL for Special Events**
For one-time or admin-controlled events, use `[MANUAL]` and trigger via command.

---

## Common Mistakes

### ❌ Missing Brackets
```
* 8h 0 0 9 * * ?    # WRONG
[*] [8h] [0 0 9 * * ?]  # CORRECT
```

### ❌ Invalid Date Format
```
[12/25/2025:12/25/2025] [8h] [NONE]  # WRONG (US format)
[2025-12-25:2025-12-25] [8h] [NONE]  # CORRECT (ISO format)
```

### ❌ Invalid Duration Unit
```
[*] [8hours] [NONE]   # WRONG
[*] [8h] [NONE]       # CORRECT
```

### ❌ Wrong Cron Syntax
```
[*] [8h] [9 * * * *]        # WRONG (missing seconds)
[*] [8h] [0 0 9 * * ?]      # CORRECT (6 fields)
```

### ❌ Invalid Day Wildcard
```
[*] [8h] [0 0 9 * * *]      # WRONG (both day fields use *)
[*] [8h] [0 0 9 * * ?]      # CORRECT (one uses ?)
```

---

## Validation

The plugin validates ACT rules when you enter them:

1. **Date Range**: Must be `*` or `YYYY-MM-DD:YYYY-MM-DD` with valid dates
2. **Duration**: Must be `*` or `<number><s|m|h|d>`
3. **Cron**: Must be `NONE`, `MANUAL`, or valid Quartz expression

If validation fails, you'll see an error message with details.

---

## Commands

*(Commands to be implemented)*

```
/ah collection <name> activate <rule-name>  # Manually activate a MANUAL rule
/ah collection <name> deactivate            # Deactivate all rules
/ah collection <name> schedule              # View rule schedule
```

---

## Integration with Progress Reset

ACT rules are **separate** from progress reset schedules:

- **ACT Rules** - Control when the collection is **available** to players
- **Progress Reset Cron** - Controls when player **progress is wiped**

These are independent systems that work together. For example:
```
ACT Rule: [*] [24h] [0 0 0 ? * MON]        # Available Mon-Tue
Progress Reset: 0 0 0 ? * MON              # Progress resets Monday
```

---

## FAQ

**Q: Can I have a collection available only on specific dates?**  
A: Yes! Use a date range: `[2025-12-25:2025-12-25] [24h] [0 0 0 25 12 ?]`

**Q: What if multiple rules are active at the same time?**  
A: That's fine! The collection remains available as long as ANY rule is active.

**Q: How do I make a collection permanently available?**  
A: Use: `[*] [*] [NONE]`

**Q: Can I change rules while they're active?**  
A: Yes, but changes may not take effect until the next server restart or rule re-evaluation.

**Q: What happens if I disable a rule that's currently active?**  
A: The collection becomes unavailable immediately if no other rules are active.

**Q: How do I test a rule before players see it?**  
A: Create the rule but leave it disabled. Enable it when ready.

---

## Advanced: Cron Reference

### Common Patterns

| Description | Cron Expression |
|-------------|----------------|
| Every minute | `0 * * * * ?` |
| Every 5 minutes | `0 */5 * * * ?` |
| Every hour | `0 0 * * * ?` |
| Every day at noon | `0 0 12 * * ?` |
| Every day at midnight | `0 0 0 * * ?` |
| Weekdays at 9 AM | `0 0 9 ? * MON-FRI` |
| Weekends at 10 AM | `0 0 10 ? * SAT,SUN` |
| First of month | `0 0 0 1 * ?` |
| Last of month | `0 0 0 L * ?` |
| Every Monday at 8 AM | `0 0 8 ? * MON` |
| 1st and 15th at noon | `0 0 12 1,15 * ?` |

### Field Ranges

| Field | Required | Values | Special |
|-------|----------|--------|---------|
| Second | Yes | 0-59 | , - * / |
| Minute | Yes | 0-59 | , - * / |
| Hour | Yes | 0-23 | , - * / |
| Day of Month | Yes | 1-31 | , - * ? / L W |
| Month | Yes | 1-12 or JAN-DEC | , - * / |
| Day of Week | Yes | 0-6 or SUN-SAT | , - * ? / L # |
| Year | No | 1970-2099 | , - * / |
