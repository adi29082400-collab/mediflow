async function login(event) {
    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value.trim();
    const loginMessage = document.getElementById("loginMessage");

    try {
        const response = await fetch("/api/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body:
                "username=" + encodeURIComponent(username) +
                "&password=" + encodeURIComponent(password)
        });

        const result = await response.json();

        if (response.ok) {
            sessionStorage.setItem("mediflowRole", result.role);
            sessionStorage.setItem("mediflowUser", result.username);

            if (result.role === "ADMIN" || result.role === "DOCTOR") {
                window.location.href = "dashboard.html";
            } else if (result.role === "PATIENT") {
                window.location.href = "patient-portal.html";
            } else {
                loginMessage.innerHTML = "Unknown user role.";
            }
        } else {
            loginMessage.innerHTML =
                result.error || "Invalid username or password.";
        }
    } catch (error) {
        console.error("Login error:", error);
        loginMessage.innerHTML =
            "Unable to connect to MediFlow server.";
    }
}


function logout() {
    sessionStorage.removeItem("mediflowRole");
    sessionStorage.removeItem("mediflowUser");
    sessionStorage.removeItem("selectedDoctor");
    sessionStorage.removeItem("lastAppointmentId");

    window.location.href = "index.html";
}


async function loadDoctors() {
    try {
        const response = await fetch("/api/doctors");

        if (!response.ok) {
            throw new Error("Failed to load doctors");
        }

        const doctors = await response.json();

        const doctorList =
            document.getElementById("doctorList");

        if (doctorList) {
            doctorList.innerHTML = "";

            if (doctors.length === 0) {
                doctorList.innerHTML =
                    "<p>No doctors available.</p>";
            } else {
                doctors.forEach(function (doctor) {
                    const card =
                        document.createElement("div");

                    card.className = "doctor-card";

                    const availability =
                        doctor.availability || "Available";

                    card.innerHTML = `
                        <h3>
                            ${escapeHtml(doctor.doctorName)}
                        </h3>

                        <p>
                            <strong>Specialization:</strong>
                            ${escapeHtml(doctor.specialization)}
                        </p>

                        <p>
                            <strong>Department:</strong>
                            ${escapeHtml(doctor.department || "N/A")}
                        </p>

                        <p>
                            <strong>Room:</strong>
                            ${escapeHtml(doctor.roomNo || "N/A")}
                        </p>

                        <p>
                            <strong>Phone:</strong>
                            ${escapeHtml(doctor.phone || "N/A")}
                        </p>

                        <p>
                            <strong>Availability:</strong>
                            ${escapeHtml(availability)}
                        </p>

                        ${
                            availability.toLowerCase() === "available"
                                ? `
                                    <button
                                        type="button"
                                        onclick="selectDoctor(
                                            ${doctor.doctorId},
                                            '${escapeSingleQuotes(
                                                doctor.doctorName
                                            )}'
                                        )"
                                    >
                                        Book Appointment
                                    </button>
                                `
                                : `
                                    <button
                                        type="button"
                                        disabled
                                    >
                                        Not Available
                                    </button>
                                `
                        }
                    `;

                    doctorList.appendChild(card);
                });
            }
        }

        const appointmentDoctor =
            document.getElementById("appointmentDoctor");

        if (appointmentDoctor) {
            appointmentDoctor.innerHTML = `
                <option value="">
                    Select Doctor
                </option>
            `;

            doctors.forEach(function (doctor) {
                const availability =
                    doctor.availability || "Available";

                if (availability.toLowerCase() === "available") {
                    const option =
                        document.createElement("option");

                    option.value = doctor.doctorId;

                    option.textContent =
                        doctor.doctorName +
                        " - " +
                        doctor.specialization;

                    appointmentDoctor.appendChild(option);
                }
            });

            const selectedDoctor =
                sessionStorage.getItem("selectedDoctor");

            if (selectedDoctor) {
                try {
                    const doctor =
                        JSON.parse(selectedDoctor);

                    appointmentDoctor.value =
                        doctor.doctorId;
                } catch (error) {
                    console.error(
                        "Invalid selected doctor:",
                        error
                    );
                }
            }
        }
    } catch (error) {
        console.error(
            "Error loading doctors:",
            error
        );

        const doctorList =
            document.getElementById("doctorList");

        if (doctorList) {
            doctorList.innerHTML = `
                <p>
                    Unable to load doctors.
                    Please make sure the server is running.
                </p>
            `;
        }

        const appointmentDoctor =
            document.getElementById("appointmentDoctor");

        if (appointmentDoctor) {
            appointmentDoctor.innerHTML = `
                <option value="">
                    Unable to load doctors
                </option>
            `;
        }
    }
}


function selectDoctor(doctorId, doctorName) {
    const doctor = {
        doctorId: doctorId,
        doctorName: doctorName
    };

    sessionStorage.setItem(
        "selectedDoctor",
        JSON.stringify(doctor)
    );

    window.location.href = "appointment.html";
}


async function loadAppointmentPatient() {
    const patientIdInput =
        document.getElementById("appointmentPatientId");

    const resultBox =
        document.getElementById("appointmentPatientResult");

    if (!patientIdInput || !resultBox) {
        return;
    }

    const patientId =
        patientIdInput.value.trim();

    if (!patientId) {
        resultBox.innerHTML =
            "<p>Please enter a Patient ID.</p>";
        return;
    }

    resultBox.innerHTML =
        "<p>Searching for patient...</p>";

    try {
        const response = await fetch(
            "/api/patients/search?id=" +
            encodeURIComponent(patientId)
        );

        const result = await response.json();

        if (!response.ok) {
            resultBox.innerHTML = `
                <p>
                    ${escapeHtml(
                        result.error || "Patient not found."
                    )}
                </p>
            `;
            return;
        }

        const patient =
            Array.isArray(result)
                ? result[0]
                : result;

        if (!patient) {
            resultBox.innerHTML =
                "<p>Patient not found.</p>";
            return;
        }

        document.getElementById(
            "appointmentPatientName"
        ).value = patient.name || "";

        document.getElementById(
            "appointmentPatientAge"
        ).value = patient.age || "";

        document.getElementById(
            "appointmentPatientGender"
        ).value = patient.gender || "";

        const diseaseInput =
            document.getElementById(
                "appointmentPatientDisease"
            );

        if (diseaseInput) {
            diseaseInput.value =
                patient.disease || "";
        }

        document.getElementById(
            "appointmentPatientPhone"
        ).value =
            patient.phone ||
            patient.phoneNumber ||
            "";

        document.getElementById(
            "appointmentPatientAddress"
        ).value =
            patient.address || "";

        const medicalHistory =
            document.getElementById(
                "appointmentPatientMedicalHistory"
            );

        if (medicalHistory) {
            medicalHistory.value =
                patient.medicalHistory || "";
        }

        const allergies =
            document.getElementById(
                "appointmentPatientAllergies"
            );

        if (allergies) {
            allergies.value =
                patient.allergies || "";
        }

        resultBox.innerHTML = `
            <p>
                Patient found successfully.
                You can edit the allowed details before booking.
            </p>
        `;
    } catch (error) {
        console.error(
            "Load appointment patient error:",
            error
        );

        resultBox.innerHTML =
            "<p>Unable to connect to server.</p>";
    }
}


async function bookAppointment(event) {
    event.preventDefault();

    const resultBox =
        document.getElementById("appointmentResult");

    const patientIdInput =
        document.getElementById("appointmentPatientId");

    const patientId =
        patientIdInput
            ? patientIdInput.value.trim()
            : "";

    const patientName =
        document.getElementById(
            "appointmentPatientName"
        ).value.trim();

    const age =
        document.getElementById(
            "appointmentPatientAge"
        ).value.trim();

    const gender =
        document.getElementById(
            "appointmentPatientGender"
        ).value.trim();

    const disease =
        document.getElementById(
            "appointmentPatientDisease"
        ).value.trim();

    const phone =
        document.getElementById(
            "appointmentPatientPhone"
        ).value.trim();

    const address =
        document.getElementById(
            "appointmentPatientAddress"
        ).value.trim();

    const medicalHistoryInput =
        document.getElementById(
            "appointmentPatientMedicalHistory"
        );

    const allergiesInput =
        document.getElementById(
            "appointmentPatientAllergies"
        );

    const medicalHistory =
        medicalHistoryInput
            ? medicalHistoryInput.value.trim()
            : "";

    const allergies =
        allergiesInput
            ? allergiesInput.value.trim()
            : "";

    const doctorId =
        document.getElementById(
            "appointmentDoctor"
        ).value;

    const appointmentDate =
        document.getElementById(
            "appointmentDate"
        ).value;

    const appointmentTime =
        document.getElementById(
            "appointmentTime"
        ).value;

    const reason =
        document.getElementById(
            "appointmentReason"
        ).value.trim();

    if (!patientName) {
        alert("Please enter the patient name.");
        return;
    }

    if (!age || Number(age) < 1 || Number(age) > 120) {
        alert(
            "Patient age must be between 1 and 120."
        );
        return;
    }

    if (!gender) {
        alert(
            "Please select the patient's gender."
        );
        return;
    }

    if (!disease) {
        alert(
            "Please enter the patient's disease."
        );
        return;
    }

    if (!phone) {
        alert(
            "Please enter the patient's phone number."
        );
        return;
    }

    if (!/^[0-9]{10}$/.test(phone)) {
        alert(
            "Please enter a valid 10 digit phone number."
        );
        return;
    }

    if (!address) {
        alert(
            "Please enter the patient's address."
        );
        return;
    }

    if (!doctorId) {
        alert("Please select a doctor.");
        return;
    }

    if (!appointmentDate) {
        alert(
            "Please select an appointment date."
        );
        return;
    }

    if (!appointmentTime) {
        alert(
            "Please select an appointment time."
        );
        return;
    }

    if (!patientId) {
        await bookAppointmentWithPatient(
            patientName,
            age,
            gender,
            disease,
            phone,
            address,
            medicalHistory,
            allergies,
            doctorId,
            appointmentDate,
            appointmentTime,
            reason,
            resultBox
        );

        return;
    }

    await updateExistingPatientBeforeBooking(
        patientId,
        patientName,
        age,
        gender,
        disease,
        phone,
        address,
        medicalHistory,
        allergies,
        doctorId,
        appointmentDate,
        appointmentTime,
        reason,
        resultBox
    );
}


async function updateExistingPatientBeforeBooking(
    patientId,
    patientName,
    age,
    gender,
    disease,
    phone,
    address,
    medicalHistory,
    allergies,
    doctorId,
    appointmentDate,
    appointmentTime,
    reason,
    resultBox
) {
    try {
        const patientData = {
            id: Number(patientId),
            name: patientName,
            age: Number(age),
            gender: gender,
            disease: disease,
            phone: phone,
            address: address,
            medicalHistory: medicalHistory,
            allergies: allergies
        };

        const updateResponse = await fetch(
            "/api/patients/" +
            encodeURIComponent(patientId),
            {
                method: "PUT",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body:
                    JSON.stringify(patientData)
            }
        );

        const updateResult =
            await updateResponse.json();

        if (!updateResponse.ok) {
            throw new Error(
                updateResult.error ||
                "Unable to update patient details."
            );
        }

        await submitAppointment(
            patientId,
            patientName,
            doctorId,
            appointmentDate,
            appointmentTime,
            reason,
            resultBox
        );
    } catch (error) {
        console.error(
            "Existing patient booking error:",
            error
        );

        resultBox.innerHTML = `
            <h2>Appointment Booking Failed</h2>
            <p>${escapeHtml(error.message)}</p>
        `;
    }
}

async function bookAppointmentWithPatient(
    patientName,
    age,
    gender,
    disease,
    phone,
    address,
    medicalHistory,
    allergies,
    doctorId,
    appointmentDate,
    appointmentTime,
    reason,
    resultBox
) {
    try {
        const patientsResponse =
            await fetch("/api/patients");

        if (!patientsResponse.ok) {
            throw new Error(
                "Unable to load patients."
            );
        }

        const patients =
            await patientsResponse.json();

        let nextPatientId = 1;

        if (
            Array.isArray(patients) &&
            patients.length > 0
        ) {
            const ids = patients
                .map(function (patient) {
                    return Number(patient.id);
                })
                .filter(function (id) {
                    return !isNaN(id);
                });

            if (ids.length > 0) {
                nextPatientId =
                    Math.max(...ids) + 1;
            }
        }

        const patientData = {
            id: nextPatientId,
            name: patientName,
            age: Number(age),
            gender: gender,
            disease: disease,
            phone: phone,
            address: address,
            medicalHistory: medicalHistory,
            allergies: allergies
        };

        const createPatientResponse =
            await fetch("/api/patients", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body:
                    JSON.stringify(patientData)
            });

        const createPatientResult =
            await createPatientResponse.json();

        if (!createPatientResponse.ok) {
            throw new Error(
                createPatientResult.error ||
                "Unable to create patient."
            );
        }

        const newPatientId =
            createPatientResult.id ||
            nextPatientId;

        await submitAppointment(
            newPatientId,
            patientName,
            doctorId,
            appointmentDate,
            appointmentTime,
            reason,
            resultBox
        );
    } catch (error) {
        console.error(
            "New patient registration error:",
            error
        );

        resultBox.innerHTML = `
            <h2>Appointment Booking Failed</h2>
            <p>${escapeHtml(error.message)}</p>
        `;
    }
}


async function submitAppointment(
    patientId,
    patientName,
    doctorId,
    appointmentDate,
    appointmentTime,
    reason,
    resultBox
) {
    try {
        const appointmentData = {
            patientId: Number(patientId),
            patientName: patientName,
            doctorId: Number(doctorId),
            appointmentDate: appointmentDate,
            appointmentTime: appointmentTime,
            reason: reason
        };

        const response =
            await fetch("/api/appointments", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body:
                    JSON.stringify(appointmentData)
            });

        const result =
            await response.json();

        if (!response.ok) {
            throw new Error(
                result.error ||
                "Unable to book appointment."
            );
        }

        if (result.appointmentId) {
            sessionStorage.setItem(
                "lastAppointmentId",
                result.appointmentId
            );
        }

        resultBox.innerHTML = `
            <h2>Appointment Submitted</h2>

            <p>
                <strong>Appointment ID:</strong>
                ${escapeHtml(
                    String(result.appointmentId)
                )}
            </p>

            <p>
                <strong>Patient ID:</strong>
                ${escapeHtml(
                    String(result.patientId)
                )}
            </p>

            <p>
                <strong>Patient Name:</strong>
                ${escapeHtml(
                    result.patientName || ""
                )}
            </p>

            <p>
                <strong>Doctor:</strong>
                ${escapeHtml(
                    result.doctorName || ""
                )}
            </p>

            <p>
                <strong>Date:</strong>
                ${escapeHtml(
                    result.appointmentDate || ""
                )}
            </p>

            <p>
                <strong>Time:</strong>
                ${escapeHtml(
                    result.appointmentTime || ""
                )}
            </p>

            <p>
                <strong>Reason:</strong>
                ${escapeHtml(
                    result.reason || "N/A"
                )}
            </p>

            <p>
                <strong>Status:</strong>
                ${escapeHtml(
                    result.status || "Pending"
                )}
            </p>

            <p>
                Your appointment has been successfully submitted.
            </p>
        `;
    } catch (error) {
        console.error(
            "Appointment booking error:",
            error
        );

        resultBox.innerHTML = `
            <h2>Appointment Booking Failed</h2>
            <p>${escapeHtml(error.message)}</p>
        `;
    }
}


async function addPatient(event) {
    event.preventDefault();

    const patientData = {
        id: Number(
            document.getElementById(
                "patientId"
            ).value
        ),

        name:
            document.getElementById(
                "patientName"
            ).value.trim(),

        age: Number(
            document.getElementById(
                "patientAge"
            ).value
        ),

        gender:
            document.getElementById(
                "patientGender"
            ).value.trim(),

        disease:
            document.getElementById(
                "patientDisease"
            ).value.trim(),

        phone:
            document.getElementById(
                "patientPhone"
            ).value.trim(),

        address:
            document.getElementById(
                "patientAddress"
            ).value.trim(),

        medicalHistory:
            document.getElementById(
                "patientMedicalHistory"
            ).value.trim(),

        allergies:
            document.getElementById(
                "patientAllergies"
            ).value.trim()
    };

    try {
        const response =
            await fetch("/api/patients", {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body:
                    JSON.stringify(patientData)
            });

        const result =
            await response.json();

        if (response.ok) {
            alert(
                "Patient added successfully!"
            );

            window.location.href =
                "view-patient.html";
        } else {
            alert(
                result.error ||
                "Failed to add patient."
            );
        }
    } catch (error) {
        console.error(
            "Add patient error:",
            error
        );

        alert(
            "Unable to connect to server."
        );
    }
}


async function loadPatients() {
    try {
        const response =
            await fetch("/api/patients");

        if (!response.ok) {
            throw new Error(
                "Failed to load patients"
            );
        }

        const patients =
            await response.json();

        const patientTable =
            document.getElementById(
                "patientTable"
            );

        if (!patientTable) {
            return;
        }

        patientTable.innerHTML = "";

        patients.forEach(function (patient) {
            const row =
                document.createElement("tr");

            row.innerHTML = `
                <td>
                    ${escapeHtml(
                        String(patient.id)
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.name || ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        String(patient.age || "")
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.gender || ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.disease || ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.phone ||
                        patient.phoneNumber ||
                        ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.address || ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.medicalHistory || ""
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        patient.allergies || ""
                    )}
                </td>
            `;

            patientTable.appendChild(row);
        });
    } catch (error) {
        console.error(
            "Load patients error:",
            error
        );
    }
}


async function searchPatient() {
    const input =
        document.getElementById(
            "searchPatientId"
        );

    const resultBox =
        document.getElementById(
            "patientResult"
        );

    if (!input || !resultBox) {
        return;
    }

    const patientId =
        input.value.trim();

    if (!patientId) {
        alert("Please enter Patient ID.");
        return;
    }

    try {
        const response = await fetch(
            "/api/patients/" +
            encodeURIComponent(patientId)
        );

        const result =
            await response.json();

        if (response.ok) {
            displayPatientResult(
                result,
                resultBox
            );
        } else {
            resultBox.innerHTML = `
                <p>
                    ${escapeHtml(
                        result.error ||
                        "Patient not found."
                    )}
                </p>
            `;
        }
    } catch (error) {
        console.error(
            "Search patient error:",
            error
        );

        resultBox.innerHTML =
            "<p>Unable to connect to server.</p>";
    }
}


async function searchPatientByNamePhone() {
    const nameInput =
        document.getElementById(
            "searchPatientName"
        );

    const phoneInput =
        document.getElementById(
            "searchPatientPhone"
        );

    const resultBox =
        document.getElementById(
            "patientResult"
        );

    if (
        !nameInput ||
        !phoneInput ||
        !resultBox
    ) {
        return;
    }

    const name =
        nameInput.value.trim();

    const phone =
        phoneInput.value.trim();

    if (!name || !phone) {
        alert(
            "Please enter both name and phone number."
        );
        return;
    }

    try {
        const response = await fetch(
            "/api/patients/search?name=" +
            encodeURIComponent(name) +
            "&phone=" +
            encodeURIComponent(phone)
        );

        const result =
            await response.json();

        if (response.ok) {
            if (Array.isArray(result)) {
                if (result.length === 1) {
                    displayPatientResult(
                        result[0],
                        resultBox
                    );
                } else {
                    displayPatientSearchResults(
                        result,
                        resultBox
                    );
                }
            } else {
                displayPatientResult(
                    result,
                    resultBox
                );
            }
        } else {
            resultBox.innerHTML = `
                <p>
                    ${escapeHtml(
                        result.error ||
                        "Patient not found."
                    )}
                </p>
            `;
        }
    } catch (error) {
        console.error(
            "Search patient error:",
            error
        );

        resultBox.innerHTML =
            "<p>Unable to connect to server.</p>";
    }
}


function displayPatientSearchResults(
    patients,
    resultBox
) {
    resultBox.innerHTML =
        "<h2>Matching Patients</h2>";

    patients.forEach(function (patient) {
        const card =
            document.createElement("div");

        card.className = "patient-card";

        card.innerHTML = `
            <h3>
                ${escapeHtml(
                    patient.name || ""
                )}
            </h3>

            <p>
                <strong>Patient ID:</strong>
                ${escapeHtml(
                    String(patient.id)
                )}
            </p>

            <p>
                <strong>Age:</strong>
                ${escapeHtml(
                    String(patient.age || "")
                )}
            </p>

            <p>
                <strong>Gender:</strong>
                ${escapeHtml(
                    patient.gender || ""
                )}
            </p>

            <p>
                <strong>Phone:</strong>
                ${escapeHtml(
                    patient.phone || ""
                )}
            </p>
        `;

        resultBox.appendChild(card);
    });
}


function displayPatientResult(
    patient,
    resultBox
) {
    if (!resultBox) {
        return;
    }

    resultBox.innerHTML = `
        <h2>Patient Information</h2>

        <p>
            <strong>Patient ID:</strong>
            ${escapeHtml(
                String(patient.id)
            )}
        </p>

        <p>
            <strong>Name:</strong>
            ${escapeHtml(
                patient.name || ""
            )}
        </p>

        <p>
            <strong>Age:</strong>
            ${escapeHtml(
                String(patient.age || "")
            )}
        </p>

        <p>
            <strong>Gender:</strong>
            ${escapeHtml(
                patient.gender || ""
            )}
        </p>

        <p>
            <strong>Disease:</strong>
            ${escapeHtml(
                patient.disease || ""
            )}
        </p>

        <p>
            <strong>Phone:</strong>
            ${escapeHtml(
                patient.phone ||
                patient.phoneNumber ||
                ""
            )}
        </p>

        <p>
            <strong>Address:</strong>
            ${escapeHtml(
                patient.address || ""
            )}
        </p>

        <p>
            <strong>Medical History:</strong>
            ${escapeHtml(
                patient.medicalHistory || ""
            )}
        </p>

        <p>
            <strong>Allergies:</strong>
            ${escapeHtml(
                patient.allergies || ""
            )}
        </p>
    `;
}

async function searchPatientForUpdate() {
    const searchType =
        document.getElementById(
            "updateSearchType"
        );

    const searchValue =
        document.getElementById(
            "updateSearchValue"
        );

    const resultBox =
        document.getElementById(
            "updateSearchResult"
        );

    const updateForm =
        document.getElementById(
            "updateFormSection"
        );

    if (
        !searchType ||
        !searchValue ||
        !resultBox
    ) {
        return;
    }

    const type =
        searchType.value;

    const value =
        searchValue.value.trim();

    if (!value) {
        alert(
            "Please enter a Patient ID or Patient Name."
        );
        return;
    }

    if (updateForm) {
        updateForm.style.display = "none";
    }

    resultBox.innerHTML =
        "<p>Searching for patient...</p>";

    try {
        let url;

        if (type === "id") {
            url =
                "/api/patients/search?id=" +
                encodeURIComponent(value);
        } else {
            url =
                "/api/patients/search?name=" +
                encodeURIComponent(value);
        }

        const response =
            await fetch(url);

        const result =
            await response.json();

        if (!response.ok) {
            resultBox.innerHTML = `
                <p>
                    ${escapeHtml(
                        result.error ||
                        "Patient not found."
                    )}
                </p>
            `;
            return;
        }

        if (
            !Array.isArray(result) ||
            result.length === 0
        ) {
            resultBox.innerHTML =
                "<p>No matching patient found.</p>";
            return;
        }

        displayUpdateSearchResults(
            result,
            resultBox
        );
    } catch (error) {
        console.error(
            "Update patient search error:",
            error
        );

        resultBox.innerHTML =
            "<p>Unable to connect to server.</p>";
    }
}


function displayUpdateSearchResults(
    patients,
    resultBox
) {
    resultBox.innerHTML = "";

    const heading =
        document.createElement("h3");

    heading.textContent =
        patients.length === 1
            ? "Patient Found"
            : "Matching Patients";

    resultBox.appendChild(heading);

    patients.forEach(function (patient) {
        const card =
            document.createElement("div");

        card.className = "patient-card";

        card.innerHTML = `
            <h3>
                ${escapeHtml(
                    patient.name || ""
                )}
            </h3>

            <p>
                <strong>Patient ID:</strong>
                ${escapeHtml(
                    String(patient.id)
                )}
            </p>

            <p>
                <strong>Age:</strong>
                ${escapeHtml(
                    String(patient.age || "")
                )}
            </p>

            <p>
                <strong>Gender:</strong>
                ${escapeHtml(
                    patient.gender || ""
                )}
            </p>

            <p>
                <strong>Disease:</strong>
                ${escapeHtml(
                    patient.disease || ""
                )}
            </p>

            <p>
                <strong>Phone:</strong>
                ${escapeHtml(
                    patient.phone || ""
                )}
            </p>

            <button
                type="button"
                class="primary-button"
                onclick="selectPatientForUpdate(
                    ${Number(patient.id)}
                )"
            >
                Select Patient
            </button>
        `;

        resultBox.appendChild(card);
    });
}


async function selectPatientForUpdate(
    patientId
) {
    const resultBox =
        document.getElementById(
            "updateSearchResult"
        );

    try {
        const response = await fetch(
            "/api/patients/" +
            encodeURIComponent(patientId)
        );

        const patient =
            await response.json();

        if (!response.ok) {
            if (resultBox) {
                resultBox.innerHTML = `
                    <p>
                        ${escapeHtml(
                            patient.error ||
                            "Unable to load patient."
                        )}
                    </p>
                `;
            }

            return;
        }

        document.getElementById(
            "updateId"
        ).value =
            patient.id || "";

        document.getElementById(
            "updateName"
        ).value =
            patient.name || "";

        document.getElementById(
            "updateAge"
        ).value =
            patient.age || "";

        document.getElementById(
            "updateGender"
        ).value =
            patient.gender || "";

        document.getElementById(
            "updateDisease"
        ).value =
            patient.disease || "";

        document.getElementById(
            "updatePhone"
        ).value =
            patient.phone ||
            patient.phoneNumber ||
            "";

        document.getElementById(
            "updateAddress"
        ).value =
            patient.address || "";

        document.getElementById(
            "updateMedicalHistory"
        ).value =
            patient.medicalHistory || "";

        document.getElementById(
            "updateAllergies"
        ).value =
            patient.allergies || "";

        const updateForm =
            document.getElementById(
                "updateFormSection"
            );

        if (updateForm) {
            updateForm.style.display =
                "block";

            updateForm.scrollIntoView({
                behavior: "smooth"
            });
        }

        if (resultBox) {
            resultBox.innerHTML = `
                <p>
                    Patient selected.
                    You can now edit the information below.
                </p>
            `;
        }
    } catch (error) {
        console.error(
            "Select patient error:",
            error
        );

        if (resultBox) {
            resultBox.innerHTML =
                "<p>Unable to load patient information.</p>";
        }
    }
}


async function updatePatient() {
    const patientIdElement =
        document.getElementById(
            "updateId"
        );

    if (!patientIdElement) {
        alert(
            "Patient ID field not found."
        );
        return;
    }

    const patientId =
        patientIdElement.value.trim();

    if (!patientId) {
        alert(
            "Please search and select a patient first."
        );
        return;
    }

    const name =
        document.getElementById(
            "updateName"
        ).value.trim();

    const age =
        document.getElementById(
            "updateAge"
        ).value.trim();

    const gender =
        document.getElementById(
            "updateGender"
        ).value;

    const disease =
        document.getElementById(
            "updateDisease"
        ).value.trim();

    const phone =
        document.getElementById(
            "updatePhone"
        ).value.trim();

    const address =
        document.getElementById(
            "updateAddress"
        ).value.trim();

    const medicalHistory =
        document.getElementById(
            "updateMedicalHistory"
        ).value.trim();

    const allergies =
        document.getElementById(
            "updateAllergies"
        ).value.trim();

    if (!name) {
        alert(
            "Please enter the patient's name."
        );
        return;
    }

    if (
        !age ||
        Number(age) < 1 ||
        Number(age) > 120
    ) {
        alert(
            "Patient age must be between 1 and 120."
        );
        return;
    }

    if (!gender) {
        alert(
            "Please select the patient's gender."
        );
        return;
    }

    if (!disease) {
        alert(
            "Please enter the patient's disease."
        );
        return;
    }

    if (!phone) {
        alert(
            "Please enter the patient's phone number."
        );
        return;
    }

    if (!/^[0-9]{10}$/.test(phone)) {
        alert(
            "Please enter a valid 10 digit phone number."
        );
        return;
    }

    if (!address) {
        alert(
            "Please enter the patient's address."
        );
        return;
    }

    const patientData = {
        id: Number(patientId),
        name: name,
        age: Number(age),
        gender: gender,
        disease: disease,
        phone: phone,
        address: address,
        medicalHistory: medicalHistory,
        allergies: allergies
    };

    const resultBox =
        document.getElementById(
            "updateResult"
        );

    try {
        const response = await fetch(
            "/api/patients/" +
            encodeURIComponent(patientId),
            {
                method: "PUT",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body:
                    JSON.stringify(patientData)
            }
        );

        const result =
            await response.json();

        if (response.ok) {
            if (resultBox) {
                resultBox.innerHTML = `
                    <h3>
                        Patient Updated Successfully
                    </h3>

                    <p>
                        Patient ID
                        <strong>
                            ${escapeHtml(patientId)}
                        </strong>
                        has been updated successfully.
                    </p>
                `;
            }

            alert(
                "Patient updated successfully!"
            );
        } else {
            if (resultBox) {
                resultBox.innerHTML = `
                    <p>
                        ${escapeHtml(
                            result.error ||
                            "Failed to update patient."
                        )}
                    </p>
                `;
            }

            alert(
                result.error ||
                "Failed to update patient."
            );
        }
    } catch (error) {
        console.error(
            "Update patient error:",
            error
        );

        if (resultBox) {
            resultBox.innerHTML =
                "<p>Unable to connect to server.</p>";
        }

        alert(
            "Unable to connect to server."
        );
    }
}


function clearUpdateForm() {
    const updateForm =
        document.getElementById(
            "updateFormSection"
        );

    const searchResult =
        document.getElementById(
            "updateSearchResult"
        );

    const updateResult =
        document.getElementById(
            "updateResult"
        );

    if (updateForm) {
        updateForm.style.display =
            "none";
    }

    if (searchResult) {
        searchResult.innerHTML = "";
    }

    if (updateResult) {
        updateResult.innerHTML = "";
    }

    const searchValue =
        document.getElementById(
            "updateSearchValue"
        );

    if (searchValue) {
        searchValue.value = "";
    }
}


async function deletePatient() {
    const patientId =
        document.getElementById(
            "deletePatientId"
        ).value.trim();

    if (!patientId) {
        alert("Please enter Patient ID.");
        return;
    }

    const confirmation =
        confirm(
            "Are you sure you want to delete this patient?"
        );

    if (!confirmation) {
        return;
    }

    try {
        const response = await fetch(
            "/api/patients/" +
            encodeURIComponent(patientId),
            {
                method: "DELETE"
            }
        );

        const result =
            await response.json();

        if (response.ok) {
            alert(
                "Patient deleted successfully!"
            );

            window.location.href =
                "dashboard.html";
        } else {
            alert(
                result.error ||
                "Failed to delete patient."
            );
        }
    } catch (error) {
        console.error(
            "Delete patient error:",
            error
        );

        alert(
            "Unable to connect to server."
        );
    }
}


function escapeHtml(value) {
    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


function escapeSingleQuotes(value) {
    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(value)
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'")
        .replace(/\r/g, "\\r")
        .replace(/\n/g, "\\n");
}

// =====================================================
// STAFF APPOINTMENT MANAGEMENT
// =====================================================

async function loadStaffAppointments() {
    const patientIdInput =
        document.getElementById(
            "staffAppointmentPatientId"
        );

    const messageBox =
        document.getElementById(
            "staffAppointmentMessage"
        );

    const listBox =
        document.getElementById(
            "staffAppointmentList"
        );

    if (!patientIdInput || !listBox) {
        return;
    }

    const patientId =
        patientIdInput.value.trim();

    if (!patientId) {
        if (messageBox) {
            messageBox.innerHTML =
                "<p>Please enter a Patient ID.</p>";
        }

        listBox.innerHTML =
            "<p>Enter a Patient ID to view appointments.</p>";

        return;
    }

    if (messageBox) {
        messageBox.innerHTML =
            "<p>Loading appointments...</p>";
    }

    listBox.innerHTML = "";

    try {
        const response = await fetch(
            "/api/appointments?patientId=" +
            encodeURIComponent(patientId),
            {
                method: "GET",
                cache: "no-store"
            }
        );

        const text =
            await response.text();

        let appointments;

        try {
            appointments =
                text ? JSON.parse(text) : [];
        } catch (error) {
            throw new Error(
                "Server returned invalid appointment data."
            );
        }

        if (!response.ok) {
            throw new Error(
                appointments.error ||
                "Unable to load appointments."
            );
        }

        if (messageBox) {
            messageBox.innerHTML = "";
        }

        if (
            !Array.isArray(appointments) ||
            appointments.length === 0
        ) {
            listBox.innerHTML =
                "<p>No appointments found for this Patient ID.</p>";

            return;
        }

        appointments.forEach(
            function (appointment) {
                createStaffAppointmentCard(
                    appointment,
                    listBox
                );
            }
        );
    } catch (error) {
        console.error(
            "Load staff appointments error:",
            error
        );

        if (messageBox) {
            messageBox.innerHTML = `
                <p>
                    ${escapeHtml(
                        error.message ||
                        "Unable to load appointments."
                    )}
                </p>
            `;
        }
    }
}


function createStaffAppointmentCard(
    appointment,
    listBox
) {
    const appointmentId =
        Number(appointment.appointmentId);

    if (
        !Number.isInteger(appointmentId) ||
        appointmentId <= 0
    ) {
        console.error(
            "Invalid appointment:",
            appointment
        );

        return;
    }

    const currentStatus =
        appointment.status || "Pending";

    const card =
        document.createElement("div");

    card.className = "patient-card";

    const heading =
        document.createElement("h3");

    heading.textContent =
        "Appointment #" + appointmentId;

    card.appendChild(heading);


    appendAppointmentInfo(
        card,
        "Patient ID",
        appointment.patientId == null
            ? "N/A"
            : appointment.patientId
    );

    appendAppointmentInfo(
        card,
        "Patient",
        appointment.patientName || ""
    );

    appendAppointmentInfo(
        card,
        "Doctor",
        appointment.doctorName || ""
    );

    appendAppointmentInfo(
        card,
        "Specialization",
        appointment.specialization || "N/A"
    );

    appendAppointmentInfo(
        card,
        "Date",
        appointment.appointmentDate || ""
    );

    appendAppointmentInfo(
        card,
        "Time",
        appointment.appointmentTime || ""
    );

    appendAppointmentInfo(
        card,
        "Reason",
        appointment.reason || "N/A"
    );


    const statusParagraph =
        document.createElement("p");

    const statusLabel =
        document.createElement("strong");

    statusLabel.textContent =
        "Current Status: ";

    const statusValue =
        document.createElement("span");

    statusValue.textContent =
        currentStatus;

    statusParagraph.appendChild(
        statusLabel
    );

    statusParagraph.appendChild(
        statusValue
    );

    card.appendChild(
        statusParagraph
    );


    const controls =
        document.createElement("div");

    controls.style.display =
        "flex";

    controls.style.gap =
        "10px";

    controls.style.flexWrap =
        "wrap";

    controls.style.alignItems =
        "center";

    controls.style.marginTop =
        "15px";


    const select =
        document.createElement("select");

    select.className =
        "appointment-status-select";


    const statuses = [
        "Pending",
        "Confirmed",
        "Completed",
        "Cancelled"
    ];


    statuses.forEach(
        function (status) {
            const option =
                document.createElement(
                    "option"
                );

            option.value =
                status;

            option.textContent =
                status;

            select.appendChild(
                option
            );
        }
    );


    if (
        statuses.includes(
            currentStatus
        )
    ) {
        select.value =
            currentStatus;
    } else {
        select.value =
            "Pending";
    }


    const updateButton =
        document.createElement("button");

    updateButton.type =
        "button";

    updateButton.className =
        "primary-button";

    updateButton.textContent =
        "Update Status";


    updateButton.addEventListener(
        "click",
        async function () {
            const newStatus =
                select.value;

            updateButton.disabled =
                true;

            updateButton.textContent =
                "Updating...";

            try {
                await updateAppointmentStatus(
                    appointmentId,
                    newStatus
                );
            } catch (error) {
                console.error(
                    "Status button error:",
                    error
                );
            } finally {
                updateButton.disabled =
                    false;

                updateButton.textContent =
                    "Update Status";
            }
        }
    );


    controls.appendChild(
        select
    );

    controls.appendChild(
        updateButton
    );

    card.appendChild(
        controls
    );

    listBox.appendChild(
        card
    );
}


function appendAppointmentInfo(
    card,
    label,
    value
) {
    const paragraph =
        document.createElement("p");

    const strong =
        document.createElement("strong");

    strong.textContent =
        label + ": ";

    paragraph.appendChild(
        strong
    );

    paragraph.appendChild(
        document.createTextNode(
            String(value)
        )
    );

    card.appendChild(
        paragraph
    );
}


// =====================================================
// UPDATE APPOINTMENT STATUS
// =====================================================

async function updateAppointmentStatus(
    appointmentId,
    status
) {
    const messageBox =
        document.getElementById(
            "staffAppointmentMessage"
        );

    if (
        !Number.isInteger(
            Number(appointmentId)
        ) ||
        Number(appointmentId) <= 0
    ) {
        throw new Error(
            "Invalid appointment ID."
        );
    }


    const validStatuses = [
        "Pending",
        "Confirmed",
        "Completed",
        "Cancelled"
    ];


    if (
        !validStatuses.includes(
            status
        )
    ) {
        throw new Error(
            "Invalid appointment status."
        );
    }


    if (messageBox) {
        messageBox.innerHTML = `
            <p>
                Updating Appointment
                #${escapeHtml(
                    String(appointmentId)
                )}...
            </p>
        `;
    }


    try {
        const response = await fetch(
            "/api/appointments/" +
            encodeURIComponent(
                appointmentId
            ),
            {
                method: "PUT",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded;charset=UTF-8"
                },

                body:
                    "status=" +
                    encodeURIComponent(
                        status
                    )
            }
        );


        const text =
            await response.text();


        let result = {};

        if (text) {
            try {
                result =
                    JSON.parse(text);
            } catch (error) {
                result = {
                    error: text
                };
            }
        }


        if (!response.ok) {
            throw new Error(
                result.error ||
                "Status update failed. HTTP " +
                response.status
            );
        }


        if (messageBox) {
            messageBox.innerHTML = `
                <p>
                    Appointment
                    #${escapeHtml(
                        String(appointmentId)
                    )}

                    changed to

                    <strong>
                        ${escapeHtml(
                            result.status ||
                            status
                        )}
                    </strong>

                    successfully.
                </p>
            `;
        }


        await loadStaffAppointments();

        return result;

    } catch (error) {
        console.error(
            "Update appointment status error:",
            error
        );


        if (messageBox) {
            messageBox.innerHTML = `
                <p>
                    Status update failed:
                    ${escapeHtml(
                        error.message ||
                        "Unknown error"
                    )}
                </p>
            `;
        }


        alert(
            "Status update failed: " +
            (
                error.message ||
                "Unknown error"
            )
        );


        throw error;
    }
}


// ============================================================
// MEDICINE AVAILABILITY
// ============================================================

async function searchMedicine() {

    const searchInput =
        document.getElementById("medicineSearch");

    const resultBox =
        document.getElementById("medicineResult");


    // This function can also exist on pages that do not
    // contain the medicine search section.
    if (!searchInput || !resultBox) {
        return;
    }


    const medicineName =
        searchInput.value.trim();


    if (!medicineName) {

        resultBox.innerHTML =
            "<p>Please enter a medicine name.</p>";

        return;
    }


    resultBox.innerHTML =
        "<p>Searching medicine...</p>";


    try {

        const response =
            await fetch(
                "/api/medicines?name=" +
                encodeURIComponent(medicineName)
            );


        const text =
            await response.text();


        let data;

        try {

            data =
                JSON.parse(text);

        } catch (error) {

            resultBox.innerHTML =
                "<p>Unable to read medicine information.</p>";

            return;
        }


        if (!response.ok) {

            resultBox.innerHTML =
                "<p>" +
                escapeHtml(
                    data.error ||
                    "Unable to search medicines."
                ) +
                "</p>";

            return;
        }


        if (!Array.isArray(data) ||
                data.length === 0) {

            resultBox.innerHTML =
                "<p>No medicine found with that name.</p>";

            return;
        }


        resultBox.innerHTML = "";


        data.forEach(function (medicine) {

            const medicineCard =
                document.createElement("div");


            medicineCard.className =
                "medicine-result-card";


            const stock =
                Number(medicine.stockQuantity) || 0;


            const isAvailable =
                stock > 0 &&
                String(medicine.availability)
                    .toLowerCase() !== "out of stock";


            const statusText =
                isAvailable
                    ? "Available"
                    : "Out of Stock";


            medicineCard.innerHTML = `
                <h3>
                    💊 ${escapeHtml(medicine.medicineName)}
                </h3>

                <p>
                    <strong>Category:</strong>
                    ${escapeHtml(medicine.category || "Not specified")}
                </p>

                <p>
                    <strong>Stock Quantity:</strong>
                    ${stock}
                </p>

                <p>
                    <strong>Availability:</strong>
                    ${escapeHtml(statusText)}
                </p>

                <p>
                    <strong>Location:</strong>
                    ${escapeHtml(medicine.location || "Not specified")}
                </p>

                <hr>
            `;


            resultBox.appendChild(
                medicineCard
            );
        });


    } catch (error) {

        console.error(
            "Medicine search error:",
            error
        );


        resultBox.innerHTML =
            "<p>Unable to connect to the medicine service.</p>";
    }
}


// ============================================================
// ALLOW ENTER KEY TO SEARCH MEDICINE
// ============================================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        const medicineInput =
            document.getElementById("medicineSearch");


        if (medicineInput) {

            medicineInput.addEventListener(
                "keydown",
                function (event) {

                    if (event.key === "Enter") {

                        event.preventDefault();

                        searchMedicine();
                    }
                }
            );
        }
    }
);


// ============================================================
// YOUR EXISTING DOM CONTENT LOADED CODE
// KEEP THIS
// ============================================================

document.addEventListener(
    "DOMContentLoaded",
    function () {
        loadPatients();
        loadDoctors();
    }
);