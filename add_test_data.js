// Script to add test employees and active locations to Firebase
const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
const serviceAccount = require('./it-adc-firebase-adminsdk.json'); // You need this file

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function addTestData() {
  const companyId = 'it-adc';
  
  // Test Employees
  const employees = [
    {
      employeeId: 'EMP001',
      uid: '',
      nameEn: 'John Smith',
      nameAr: 'جون سميث',
      email: 'john.smith@company.com',
      phoneNumber: '+966501234567',
      role: 'admin',
      departmentEn: 'IT Department',
      departmentAr: 'قسم تقنية المعلومات',
      active: true,
      avatarURL: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    {
      employeeId: 'EMP002',
      uid: '',
      nameEn: 'Sarah Johnson',
      nameAr: 'سارة جونسون',
      email: 'sarah.j@company.com',
      phoneNumber: '+966502345678',
      role: 'supervisor',
      departmentEn: 'Operations',
      departmentAr: 'العمليات',
      active: true,
      avatarURL: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    {
      employeeId: 'EMP003',
      uid: '',
      nameEn: 'Ahmed Ali',
      nameAr: 'أحمد علي',
      email: 'ahmed.ali@company.com',
      phoneNumber: '+966503456789',
      role: 'employee',
      departmentEn: 'Sales',
      departmentAr: 'المبيعات',
      active: true,
      avatarURL: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }
  ];

  // Active Locations (Riyadh, Saudi Arabia area)
  const activeLocations = [
    {
      employeeId: 'EMP001',
      location: new admin.firestore.GeoPoint(24.7136, 46.6753), // Riyadh center
      placeName: 'King Fahd Road, Riyadh',
      previousPlaceName: null,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      checkInTime: admin.firestore.FieldValue.serverTimestamp()
    },
    {
      employeeId: 'EMP002',
      location: new admin.firestore.GeoPoint(24.7243, 46.6875), // North Riyadh
      placeName: 'Olaya District, Riyadh',
      previousPlaceName: null,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      checkInTime: admin.firestore.FieldValue.serverTimestamp()
    },
    {
      employeeId: 'EMP003',
      location: new admin.firestore.GeoPoint(24.6982, 46.6842), // South Riyadh
      placeName: 'Al Malqa, Riyadh',
      previousPlaceName: null,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      checkInTime: admin.firestore.FieldValue.serverTimestamp()
    }
  ];

  try {
    console.log('Adding test employees...');
    for (const employee of employees) {
      await db.collection(`companies/${companyId}/employees`).doc(employee.employeeId).set(employee);
      console.log(`✅ Added employee: ${employee.nameEn}`);
    }

    console.log('\nAdding active locations...');
    for (const location of activeLocations) {
      await db.collection(`companies/${companyId}/activeLocations`).doc(location.employeeId).set(location);
      console.log(`✅ Added location for: ${location.employeeId}`);
    }

    console.log('\n🎉 Test data added successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error adding test data:', error);
    process.exit(1);
  }
}

addTestData();
