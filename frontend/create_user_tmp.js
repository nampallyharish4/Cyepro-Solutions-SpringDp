const { createClient } = require('@supabase/supabase-js');

const supabaseUrl = 'https://llvedrcxpocdpguhxkql.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxsdmVkcmN4cG9jZHBndWh4a3FsIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3Mjc3NjQzNCwiZXhwIjoyMDg4MzUyNDM0fQ.kwkg3Wl8YV5F5Bu2ZC-xbaIpc59y3SD7PIYK84ijCZU'; // Service Role Key

const supabase = createClient(supabaseUrl, supabaseKey);

async function createTestUser() {
  const email = 'testuser_antigravity@gmail.com';
  const password = 'Password111';

  console.log(`Attempting to create user: ${email}`);

  // Using admin API directly
  const { data, error } = await supabase.auth.admin.createUser({
    email: email,
    password: password,
    email_confirm: true,
    user_metadata: { role: 'admin' }
  });

  if (error) {
    if (error.message.includes('already exists')) {
      console.log('User already exists, we can use it.');
    } else {
      console.error('Error creating user:', error.message);
      process.exit(1);
    }
  } else {
    console.log('User created successfully:', data.user.id);
  }
}

createTestUser();
