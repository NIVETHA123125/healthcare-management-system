import requests
import re
import random

BASE_URL = "http://localhost:8080"

def run_tests():
    s = requests.Session()
    
    unique_id = random.randint(10000, 99999)
    email = f"jane.doe.{unique_id}@healthcare.com"
    phone = f"98765{random.randint(10000, 99999)}"
    
    # 1. Register a new patient
    print(f"Registering a new patient with email {email}...")
    reg_data = {
        "fullName": "Jane Doe",
        "email": email,
        "phone": phone,
        "gender": "Female",
        "dob": "1995-05-15",
        "address": "123 Medical St",
        "password": "PatientPassword@123",
        "confirmPassword": "PatientPassword@123"
    }
    r = s.post(f"{BASE_URL}/register/save", data=reg_data)
    
    print("Logging in as patient...")
    login_data = {
        "username": email,
        "password": "PatientPassword@123",
        "role": "PATIENT"
    }
    r = s.post(f"{BASE_URL}/login", data=login_data, allow_redirects=False)
    if r.status_code != 302:
        print("-> FAIL: Patient login failed.")
        return
    
    # Now let's login as admin to create a billing invoice for Jane Doe
    print("Logging in as Admin...")
    s_admin = requests.Session()
    r = s_admin.post(f"{BASE_URL}/login", data={
        "username": "admin@healthcare.com",
        "password": "Admin@123",
        "role": "ADMIN"
    }, allow_redirects=False)
    if r.status_code != 302:
        print("-> FAIL: Admin login failed.")
        return
        
    # Get Patient ID
    print("Fetching patient list...")
    r = s_admin.get(f"{BASE_URL}/patients")
    
    # Look for our newly created patient's ID in the HTML
    # It contains th:href="@{/patients/edit/{id}(id=${patient.id})}" -> /patients/edit/123
    # Let's search for the ID corresponding to our registered email or just search for /patients/edit/(\d+)
    matches = re.findall(r"patients/edit/(\d+)", r.text)
    if not matches:
        print("-> FAIL: Patient ID not found in admin list.")
        return
    
    # Let's use the most recently registered patient ID (which is usually the last one or we can find it by searching for the row containing our email)
    # Let's parse HTML rows to find the exact patient ID for email
    patient_id = None
    rows = re.findall(r"<tr>(.*?)</tr>", r.text, re.DOTALL)
    for row in rows:
        if email in row:
            m = re.search(r"patients/edit/(\d+)", row)
            if m:
                patient_id = m.group(1)
                break
                
    if not patient_id:
        # Fallback to last match
        patient_id = matches[-1]
        
    print(f"Found patient ID: {patient_id}")
    
    # Create Invoice for patient
    print("Creating billing invoice...")
    bill_data = {
        "patientId": patient_id,
        "amount": "150.00",
        "paymentMethod": "CARD",
        "status": "PENDING"
    }
    r = s_admin.post(f"{BASE_URL}/payments/save", data=bill_data, allow_redirects=False)
    if r.status_code != 302:
        print("-> FAIL: Failed to save invoice.")
        return
        
    # Get Invoice ID from patient payments list
    print("Fetching patient payments...")
    r = s.get(f"{BASE_URL}/patient/payments")
    match = re.search(r"/payments/pay/(\d+)", r.text)
    if not match:
        print("-> FAIL: Pay Now button / Invoice ID not found.")
        return
    invoice_id = match.group(1)
    print(f"Found Invoice ID: {invoice_id}")
    
    # Test 1: Wrong card details
    print("Testing payment with incorrect card details...")
    r = s.post(f"{BASE_URL}/patient/payments/process/{invoice_id}", data={
        "paymentMethod": "CARD",
        "cardHolderName": "Wrong Holder",
        "cardNumber": "4111111111111111",
        "cvv": "123",
        "pin": "1234"
    })
    if "Invalid Card Details" in r.text:
        print("-> SUCCESS: Received 'Invalid Card Details' error message.")
    else:
        print("-> FAIL: Unexpected response for wrong card details.")
        
    # Test 2: Correct details
    print("Testing payment with correct card details...")
    r = s.post(f"{BASE_URL}/patient/payments/process/{invoice_id}", data={
        "paymentMethod": "CARD",
        "cardHolderName": "Demo User",
        "cardNumber": "4111111111111111",
        "cvv": "123",
        "pin": "1234"
    })
    if "Payment Successful!" in r.text and "TXN-" in r.text:
        print("-> SUCCESS: Payment processed successfully with generated Transaction ID!")
    else:
        print("-> FAIL: Payment failed on valid credentials.")

if __name__ == "__main__":
    run_tests()
