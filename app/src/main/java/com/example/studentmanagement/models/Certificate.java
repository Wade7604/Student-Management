    package com.example.studentmanagement.models;

    import com.google.firebase.database.Exclude;
    import com.google.firebase.database.IgnoreExtraProperties;

    import java.io.Serializable;
    import java.util.HashMap;
    import java.util.Map;

    @IgnoreExtraProperties
    public class Certificate implements Serializable {
        private String id;
        private String certificateName;
        private String issuingAuthority;
        private String issueDate;
        private String expiryDate;
        private String studentName;
        private String description;
        private String studentId;

        // Required empty constructor for Firebase
        public Certificate() {
        }

        public Certificate(String certificateName, String issuingAuthority, String issueDate, String expiryDate, String description, String studentId) {
            this.certificateName = certificateName;
            this.issuingAuthority = issuingAuthority;
            this.issueDate = issueDate;
            this.expiryDate = expiryDate;
            this.description = description;
            this.studentId = studentId;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCertificateName() {
            return certificateName;
        }

        public void setCertificateName(String certificateName) {
            this.certificateName = certificateName;
        }

        public String getIssuingAuthority() {
            return issuingAuthority;
        }
public void setStudentName(String studentName){
            this.studentName = studentName;
}
        public void setIssuingAuthority(String issuingAuthority) {
            this.issuingAuthority = issuingAuthority;
        }

        public String getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(String issueDate) {
            this.issueDate = issueDate;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        @Exclude
        public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();
            result.put("certificateName", certificateName);
            result.put("issuingAuthority", issuingAuthority);
            result.put("issueDate", issueDate);
            result.put("expiryDate", expiryDate);
            result.put("description", description);
            result.put("studentId", studentId);
            return result;
        }
    }