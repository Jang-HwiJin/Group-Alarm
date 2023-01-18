# GroupAlarm


## Inspiration:
As college students, we have so many things to do, a lot of which are actually shared activities with our friends/roommates/colleagues/etc. We never want to be late, but as part of human nature, it’s so easy to forget setting an alarm and end up being late or left behind (we all know how embarrassing it is!). Group Alarm is the solution for this long-standing problem. With Group Alarm, you can make synchronized alarms with your friends, thereby minimizing the risk of being late. You can sleep through your own alarm, but it’s almost impossible for everyone in your group to miss the alarm, and you will be waken up in one way or another. With GroupAlarm, you’ll no longer be late, or left behind.

## Functionalities
1. Login Page: Signup, Login
2. Dashboard: 
+ Create/delete an alarm (only for the alarm’s owner)
+ Accept/decline alarms created by others. If an alarm is accepted, it is automatically set on the user's phone
+ View and manage all alarms
+ Toggle on/off an alarm (=register/unregister oneself from the group)
3. Alarm Page: View more details of a specific alarm (time, name, description, list of users registered for the alarm)
4. Stop Alarm Page: Stop an alarm when it's firing
6. Profile Page: View account
7. Profile Edit Page: Edit profile settings such as username and profile picture
9. Friends Page: Manage friends and friend requests
8. Settings Page: Edit settings such as notifications, signing out, and etc

## Technologies:
+ Frontend: RecyclerView, Fragments, DialogueFragment, ConstraintLayout, LinearLayout
+ Backend:
	+ Firebase Firestore: persistent data storage & real-time updates
	+ Firebase RealTime: real-time updates & user presence management
	+ Firebase Cloud Storage: file storage
	+ Firebase Authentication: authentication & authorization
	+ Pending Intent, BroadcastReceiver, AlarmManager: alarm scheduling & firing


## Technical Difficulties:
1. How to access the system alarm to set an alarm directly from our application
2. Create groups & send/accept/decline invitations

## What’s next?
+ Customizable Invitation Group: Users can customize the invitation list for a newly created alarm
+ Group Messaging: Users in the same group can chat and send media/files
=======
# Group-Alarm
