# TaMa – Task Management Application

TaMa là ứng dụng web quản lý công việc cá nhân được xây dựng theo mô hình bảng, danh sách và thẻ công việc. Dự án lấy cảm hứng từ cách tổ chức công việc của Trello, tập trung vào các chức năng quản lý công việc cơ bản, trực quan và dễ sử dụng.

Dự án được thực hiện trong khuôn khổ học phần **Thực hành lập trình ứng dụng Java** tại Trường Đại học Công nghệ TP.HCM – HUTECH.

> Đây là dự án phục vụ mục đích học tập, trình diễn và bảo vệ đồ án môn học. Dự án chưa được thiết kế để triển khai trong môi trường production.

---

## Chức năng chính

### Xác thực và phân quyền

- Đăng ký tài khoản.
- Đăng nhập và đăng xuất.
- Mã hóa mật khẩu bằng BCrypt.
- Duy trì trạng thái đăng nhập bằng session.
- Phân quyền người dùng:
  - `USER`: sử dụng các chức năng quản lý công việc cá nhân.
  - `ADMIN`: quản lý tài khoản người dùng.

### Quản lý Board

- Tạo Board mới.
- Xem danh sách Board của người dùng.
- Cập nhật thông tin Board.
- Xóa Board.
- Thiết lập màu nền hoặc ảnh nền cho Board.

### Quản lý List

- Tạo List trong Board.
- Đổi tên List.
- Xóa List.
- Sắp xếp các List theo vị trí.

### Quản lý Card

- Tạo Card trong List.
- Cập nhật tiêu đề và nội dung Card.
- Xóa Card.
- Di chuyển Card giữa các List trong cùng một Board.
- Sắp xếp Card bằng thao tác kéo thả.
- Lưu thứ tự Card bằng thuộc tính `position`.

### Chi tiết Card

- Mô tả công việc.
- Ngày hết hạn.
- Đánh dấu hoàn thành.
- Thời điểm hoàn thành.
- Mức độ ưu tiên:
  - `LOW`
  - `MEDIUM`
  - `HIGH`
  - `URGENT`
- Checklist công việc.
- Bình luận.
- Chỉnh sửa và xóa bình luận.
- Gắn Label cho Card.

### Quản lý Label

- Tạo Label cá nhân.
- Cập nhật Label.
- Xóa Label.
- Tái sử dụng Label cho nhiều Card.
- Quản lý màu sắc của Label.

### Quản trị người dùng

- Xem danh sách tài khoản.
- Xem thông tin người dùng.
- Quản lý trạng thái hoặc quyền của tài khoản theo phạm vi hệ thống.
- Giới hạn quyền truy cập các chức năng quản trị đối với người dùng thông thường.

---

## Công nghệ sử dụng

### Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Jakarta Validation
- Maven
- Lombok

### Frontend

- HTML
- CSS
- JavaScript
- Fetch API
- Thymeleaf
- Thymeleaf Layout Dialect
- Bootstrap

### Cơ sở dữ liệu

- MySQL

### Công cụ phát triển

- IntelliJ IDEA
- HeidiSQL
- Laragon
- Postman
- Git
- GitHub

---

## Kiến trúc hệ thống

Dự án được tổ chức theo kiến trúc phân tầng:

```text
Client
   │
   ▼
Controller / REST Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
MySQL Database
```

Vai trò của từng tầng:

- **Controller:** tiếp nhận request và trả về giao diện hoặc dữ liệu JSON.
- **Service:** xử lý nghiệp vụ của hệ thống.
- **Repository:** truy cập dữ liệu thông qua Spring Data JPA.
- **Entity:** ánh xạ các đối tượng Java với bảng trong cơ sở dữ liệu.
- **DTO:** truyền dữ liệu giữa client và server trong các API.
- **Thymeleaf:** quản lý layout và khung giao diện chung.
- **JavaScript Fetch API:** gọi REST API và cập nhật giao diện mà không cần tải lại toàn bộ trang.

Dự án vẫn sử dụng mô hình MVC, trong đó Thymeleaf chịu trách nhiệm cung cấp layout và trang ban đầu, còn JavaScript gọi API để thực hiện phần lớn thao tác nghiệp vụ.

---

## Quy tắc nghiệp vụ chính

- Mỗi Board chỉ thuộc sở hữu của một người dùng.
- Người dùng chỉ được quản lý Board thuộc sở hữu của mình.
- Card chỉ được di chuyển giữa các List trong cùng một Board.
- Chưa hỗ trợ di chuyển Card giữa hai Board khác nhau.
- List và Card sử dụng thuộc tính `position` bắt đầu từ `0`.
- Khi thay đổi vị trí, hệ thống chuẩn hóa lại thứ tự của các phần tử.
- Label thuộc sở hữu của người dùng và có thể được tái sử dụng.
- Card chưa hỗ trợ phân công cho nhiều thành viên.
- Dữ liệu được xóa trực tiếp, chưa triển khai soft delete.
- Hệ thống chưa lưu lịch sử thay đổi của Card.

---

## Yêu cầu môi trường

Trước khi chạy dự án, cần cài đặt:

- JDK 17
- MySQL
- Git
- IntelliJ IDEA hoặc IDE hỗ trợ Maven

Có thể kiểm tra phiên bản Java bằng lệnh:

```bash
java -version
```

Kết quả cần hiển thị Java 17.

---

## Cài đặt dự án

### 1. Clone repository

```bash
git clone https://github.com/longchau5823/tama_task_management.git
cd tama_task_management
```

Hoặc tải source code dưới dạng ZIP từ GitHub và giải nén.

---

### 2. Khởi tạo cơ sở dữ liệu

Mở MySQL bằng Laragon, HeidiSQL hoặc công cụ quản trị tương đương.

Import file:

```text
database/tama_db.sql
```

Script sẽ khởi tạo database:

```text
tama_db
```

Có thể import bằng HeidiSQL:

1. Kết nối tới MySQL.
2. Chọn **File → Load SQL file**.
3. Chọn file `database/tama_db.sql`.
4. Thực thi script.
5. Kiểm tra các bảng đã được tạo.

---

### 3. Cấu hình kết nối database

Dự án hỗ trợ cấu hình kết nối thông qua biến môi trường.

Các biến có thể sử dụng:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Ví dụ:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/tama_db}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

Với cấu hình MySQL mặc định của Laragon:

```text
Database: tama_db
Username: root
Password: để trống
Port: 3306
```

Không nên đưa mật khẩu database thật vào repository.

---

### 4. Chạy dự án

#### Sử dụng IntelliJ IDEA

1. Mở IntelliJ IDEA.
2. Chọn **Open**.
3. Chọn thư mục dự án.
4. Chờ Maven tải các dependency.
5. Kiểm tra Project SDK đang sử dụng JDK 17.
6. Chạy class chứa annotation:

```java
@SpringBootApplication
```

#### Sử dụng Maven Wrapper trên Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Sử dụng Git Bash, Linux hoặc macOS

```bash
./mvnw spring-boot:run
```

Sau khi ứng dụng khởi động thành công, truy cập:

```text
http://localhost:8080
```

---

## Tài khoản sử dụng

Người dùng có thể tạo tài khoản mới tại:

```text
http://localhost:8080/register
```

Sau khi đăng ký, tài khoản mặc định được cấp quyền người dùng thông thường.

Repository không công khai mật khẩu hoặc tài khoản quản trị cố định. Để kiểm thử chức năng quản trị, cần cấp quyền `ADMIN` cho tài khoản phù hợp trong cơ sở dữ liệu.

Không nên lưu tài khoản cá nhân, mật khẩu thật hoặc dữ liệu người dùng thật trong file SQL.

---

## Cấu trúc thư mục

```text
tama_task_management/
├── .mvn/
│   └── wrapper/
│
├── database/
│   └── tama_db.sql
│
├── src/
│   └── main/
│       ├── java/
│       │   └── ...
│       │       ├── config/
│       │       ├── controller/
│       │       ├── dto/
│       │       ├── entity/
│       │       ├── repository/
│       │       ├── security/
│       │       └── service/
│       │
│       └── resources/
│           ├── static/
│           │   ├── css/
│           │   ├── images/
│           │   └── js/
│           │
│           ├── templates/
│           └── application.properties
│
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

Cấu trúc thực tế có thể khác một phần tùy theo package hiện tại của dự án.

---

## Phạm vi hiện tại

Phiên bản hiện tại tập trung vào quản lý công việc cá nhân.

Các chức năng chưa nằm trong phạm vi:

- Board có nhiều thành viên.
- Phân công Card cho thành viên.
- Chia sẻ Board.
- Di chuyển Card giữa nhiều Board.
- Thông báo thời gian thực.
- Email nhắc ngày hết hạn.
- Lịch sử hoạt động.
- Soft delete và khôi phục dữ liệu.
- Đồng bộ dữ liệu theo thời gian thực.
- Ứng dụng di động.
- Triển khai production.

---

## Hướng phát triển

Một số hướng có thể tiếp tục phát triển:

- Hỗ trợ nhiều thành viên trong Board.
- Phân quyền chủ sở hữu, quản trị viên và thành viên.
- Phân công Card cho người dùng.
- Gửi thông báo khi Card gần hết hạn.
- Bổ sung activity log.
- Tìm kiếm và lọc Card.
- Lưu trữ file đính kèm.
- Soft delete và khôi phục dữ liệu.
- Viết đầy đủ unit test và integration test.
- Docker hóa ứng dụng và database.
- Triển khai ứng dụng lên môi trường cloud.
- Xây dựng frontend riêng bằng React hoặc Vue.

---

## Thành viên thực hiện

- **Châu Thuyên Long** – Trưởng nhóm, phát triển chính
- **<Tên thành viên 2>** – Phân tích và thiết kế hệ thống
- **<Tên thành viên 3>** – Tài liệu và trình bày

Trường Đại học Công nghệ TP.HCM – HUTECH  
Ngành Công nghệ thông tin – Chuyên ngành Công nghệ phần mềm

---

## Ghi chú

Dự án được xây dựng nhằm mục đích học tập và thực hành các kiến thức:

- Spring Boot
- Spring MVC
- Spring Security
- RESTful API
- Spring Data JPA
- MySQL
- Thymeleaf
- JavaScript Fetch API
- Thiết kế và tổ chức ứng dụng theo kiến trúc phân tầng

Một số thành phần có thể tiếp tục được chỉnh sửa và hoàn thiện trong các phiên bản sau.
