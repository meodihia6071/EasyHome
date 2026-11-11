# EasyHome - cháº¡y local vá»›i XAMPP / MariaDB (Phase A)

Ãp dá»¥ng cho branch `main` cá»§a `meodihia6071/EasyHome`. **KhÃ´ng thay Ä‘á»•i business logic.**

## 1. Chuáº©n bá»‹
- CÃ i vÃ  báº­t **MySQL** trong XAMPP Control Panel (thá»±c táº¿ lÃ  MariaDB trong nhiá»u báº£n XAMPP).
- VÃ o `http://localhost/phpmyadmin` vÃ  táº¡o database `easyhome` dÃ¹ng `utf8mb4_unicode_ci`; hoáº·c cháº¡y file `database.sql` trong tab SQL.
- XAMPP thÆ°á»ng dÃ¹ng user `root`, máº­t kháº©u rá»—ng cho local. Náº¿u báº¡n Ä‘Ã£ Ä‘áº·t máº­t kháº©u hoáº·c Ä‘á»•i cá»•ng, dÃ¹ng `DB_USER`, `DB_PASSWORD`, `DB_URL` Ä‘á»ƒ cáº¥u hÃ¬nh; khÃ´ng lÆ°u máº­t kháº©u vÃ o Git.
- **KhÃ´ng báº­t Docker MySQL cÃ¹ng cá»•ng MariaDB XAMPP**. Docker Compose cá»§a dá»± Ã¡n váº«n sá»­ dá»¥ng MySQL 8.4 nhÆ° trÆ°á»›c.

## 2. Cháº¡y vÃ  kiá»ƒm thá»­
Má»Ÿ terminal trong `backend`:

```powershell
mvn clean test
mvn spring-boot:run
```

Test `MarketplaceFlowTest` Ä‘Ã£ cÃ³ URL H2 in-memory riÃªng nÃªn khÃ´ng cáº§n báº­t XAMPP Ä‘á»ƒ cháº¡y `mvn clean test` (cáº§n dependencies Maven).

Terminal khÃ¡c trong `frontend`:

```powershell
npm ci
npm run dev -- --host 0.0.0.0
```

Website `http://localhost:5173`; API `http://localhost:8081/api/listings`.
Vite Ä‘Ã£ proxy `/api` Ä‘áº¿n `localhost:8081` trÃªn chÃ­nh mÃ¡y phÃ¡t triá»ƒn.

## 3. Xem dá»¯ liá»‡u / ERD
VÃ o `http://localhost/phpmyadmin`, chá»n database `easyhome`, xem báº£ng vÃ  dá»¯ liá»‡u. Äá»ƒ xem sÆ¡ Ä‘á»“, dÃ¹ng tab `Designer` (phá»¥ thuá»™c phiÃªn báº£n vÃ  cáº¥u hÃ¬nh phpMyAdmin); cÅ©ng cÃ³ thá»ƒ dÃ¹ng DBeaver Community káº¿t ná»‘i MariaDB `127.0.0.1:3306` Ä‘á»ƒ xem ERD dá»… hÆ¡n.

## 4. LÆ°u Ã½
- File H2 cÅ© (`backend/data/*.mv.db`) **khÃ´ng Ä‘Æ°á»£c nháº­p tá»± Ä‘á»™ng** sang MariaDB; hÃ£y sao lÆ°u náº¿u cáº§n giá»¯ dá»¯ liá»‡u.
- JPA `ddl-auto=update` sáº½ tá»± táº¡o/cáº­p nháº­t báº£ng khi backend cháº¡y trong mÃ´i trÆ°á»ng local; **khÃ´ng dÃ¹ng cÃ¡ch nÃ y Ä‘á»ƒ quáº£n lÃ½ schema production**.
- Náº¿u cá»•ng XAMPP khÃ¡c `3306`, Ä‘áº·t `DB_URL=jdbc:mariadb://127.0.0.1:<PORT>/easyhome` trong terminal trÆ°á»›c khi cháº¡y backend.
- Máº­t kháº©u seed/demo vÃ  mock-payment chá»‰ dÃ¹ng Ä‘á»ƒ thá»­ nghiá»‡m ná»™i bá»™, khÃ´ng má»Ÿ cÃ´ng khai ra Internet.