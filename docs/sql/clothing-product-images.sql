-- Replace sample catalog illustrations with distinct local product images.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @images TABLE(slug VARCHAR(200) PRIMARY KEY,image_url VARCHAR(2048));
INSERT @images VALUES
 ('an-nhien-polo','/images/products/ao-polo/an-nhien-polo.jpg'),
 ('an-nhien-polo-02','/images/products/ao-polo/an-nhien-polo-02.jpg'),
 ('an-nhien-polo-03','/images/products/ao-polo/an-nhien-polo-03.jpg'),
 ('an-nhien-polo-04','/images/products/ao-polo/an-nhien-polo-04.jpg'),
 ('an-nhien-polo-05','/images/products/ao-polo/an-nhien-polo-05.jpg'),
 ('an-nhien-polo-06','/images/products/ao-polo/an-nhien-polo-06.jpg'),
 ('an-nhien-polo-07','/images/products/ao-polo/an-nhien-polo-07.jpg'),
 ('an-nhien-polo-08','/images/products/ao-polo/an-nhien-polo-08.jpg'),
 ('an-nhien-polo-09','/images/products/ao-polo/an-nhien-polo-09.jpg'),
 ('an-nhien-polo-10','/images/products/ao-polo/an-nhien-polo-10.jpg'),
 ('moc-so-mi-oxford','/images/products/ao-so-mi/moc-so-mi-oxford.jpg'),
 ('moc-so-mi-02','/images/products/ao-so-mi/moc-so-mi-02.jpg'),
 ('moc-so-mi-03','/images/products/ao-so-mi/moc-so-mi-03.jpg'),
 ('moc-so-mi-04','/images/products/ao-so-mi/moc-so-mi-04.jpg'),
 ('moc-so-mi-05','/images/products/ao-so-mi/moc-so-mi-05.jpg'),
 ('moc-so-mi-06','/images/products/ao-so-mi/moc-so-mi-06.jpg'),
 ('moc-so-mi-07','/images/products/ao-so-mi/moc-so-mi-07.jpg'),
 ('moc-so-mi-08','/images/products/ao-so-mi/moc-so-mi-08.jpg'),
 ('moc-so-mi-09','/images/products/ao-so-mi/moc-so-mi-09.jpg'),
 ('moc-so-mi-10','/images/products/ao-so-mi/moc-so-mi-10.jpg'),
 ('pho-denim-quan-jeans','/images/products/quan-jeans/pho-denim-quan-jeans.jpg'),
 ('pho-denim-jeans-02','/images/products/quan-jeans/pho-denim-jeans-02.jpg'),
 ('pho-denim-jeans-03','/images/products/quan-jeans/pho-denim-jeans-03.jpg'),
 ('pho-denim-jeans-04','/images/products/quan-jeans/pho-denim-jeans-04.jpg'),
 ('pho-denim-jeans-05','/images/products/quan-jeans/pho-denim-jeans-05.jpg'),
 ('pho-denim-jeans-06','/images/products/quan-jeans/pho-denim-jeans-06.jpg'),
 ('pho-denim-jeans-07','/images/products/quan-jeans/pho-denim-jeans-07.jpg'),
 ('pho-denim-jeans-08','/images/products/quan-jeans/pho-denim-jeans-08.jpg'),
 ('pho-denim-jeans-09','/images/products/quan-jeans/pho-denim-jeans-09.jpg'),
 ('pho-denim-jeans-10','/images/products/quan-jeans/pho-denim-jeans-10.jpg');
IF (SELECT COUNT(*) FROM @images a JOIN dbo.products p ON p.slug=a.slug)<>30
    THROW 50001,'Expected all thirty sample products before updating images.',1;
UPDATE p SET image_url=a.image_url,updated_at=SYSUTCDATETIME()
FROM dbo.products p JOIN @images a ON a.slug=p.slug
WHERE p.image_url<>a.image_url OR p.image_url IS NULL;
COMMIT;
SELECT c.name,COUNT(*) AS products,COUNT(DISTINCT p.image_url) AS distinct_images
FROM dbo.products p JOIN @images a ON a.slug=p.slug JOIN dbo.categories c ON c.id=p.category_id
GROUP BY c.name;
