const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkFirestore() {
  console.log('\n=== FIRESTORE DATABASE CHECK ===\n');
  
  try {
    // Check Employees
    console.log('📋 EMPLOYEES COLLECTION:');
    const employeesSnapshot = await db.collection('employees').get();
    console.log(`   Total employees: ${employeesSnapshot.size}`);
    employeesSnapshot.docs.slice(0, 3).forEach(doc => {
      const data = doc.data();
      console.log(`   - ${data.displayName || data.email} (${data.employeeId}) [${data.role}]`);
    });
    
    // Check Attendance (recent)
    console.log('\n📅 ATTENDANCE COLLECTION:');
    const attendanceSnapshot = await db.collection('attendance')
      .orderBy('checkInTime', 'desc')
      .limit(5)
      .get();
    console.log(`   Total records: ${attendanceSnapshot.size} (showing last 5)`);
    attendanceSnapshot.docs.forEach(doc => {
      const data = doc.data();
      const checkInTime = data.checkInTime?.toDate().toLocaleString();
      console.log(`   - ${data.employeeName || data.employeeId}: ${data.status} at ${checkInTime}`);
    });
    
    // Check Active Locations
    console.log('\n📍 ACTIVE LOCATIONS COLLECTION:');
    const locationsSnapshot = await db.collection('activeLocations').get();
    console.log(`   Total active locations: ${locationsSnapshot.size}`);
    if (locationsSnapshot.size === 0) {
      console.log('   ⚠️  NO ACTIVE LOCATIONS FOUND!');
      console.log('   This is why employees don\'t show on the map.');
    } else {
      locationsSnapshot.docs.forEach(doc => {
        const data = doc.data();
        const timestamp = data.timestamp?.toDate().toLocaleString();
        let location = 'unknown';
        if (data.location && data.location.latitude) {
          location = `${data.location.latitude.toFixed(4)}, ${data.location.longitude.toFixed(4)}`;
        }
        console.log(`   - ${doc.id}: ${data.placeName || 'No place'} at ${location} (${timestamp})`);
      });
    }
    
    // Check checked-in employees
    console.log('\n✅ CURRENTLY CHECKED-IN EMPLOYEES:');
    const checkedInSnapshot = await db.collection('attendance')
      .where('status', '==', 'checked_in')
      .get();
    console.log(`   Total checked-in: ${checkedInSnapshot.size}`);
    if (checkedInSnapshot.size === 0) {
      console.log('   ⚠️  NO ONE IS CHECKED IN!');
      console.log('   Check-in first to see employees on the map.');
    } else {
      checkedInSnapshot.docs.forEach(doc => {
        const data = doc.data();
        console.log(`   - ${data.employeeName || data.employeeId} (${data.employeeId})`);
      });
    }
    
    // Cross-check: checked-in vs activeLocations
    if (checkedInSnapshot.size > 0 && locationsSnapshot.size === 0) {
      console.log('\n❌ ISSUE FOUND:');
      console.log('   Employees are checked-in but no activeLocations exist!');
      console.log('   The updateActiveLocation() function may not be working.');
    } else if (checkedInSnapshot.size > 0 && locationsSnapshot.size > 0) {
      console.log('\n✅ DATA LOOKS GOOD:');
      console.log(`   ${checkedInSnapshot.size} checked-in, ${locationsSnapshot.size} active locations`);
    }
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
  
  process.exit(0);
}

checkFirestore();
