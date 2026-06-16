require("dotenv").config();
const sql = require("mssql");
const dbConfig = {
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER,
    database: process.env.DB_NAME,
    options: { encrypt: false, trustServerCertificate: true }
};

const tablesSql = [
    // 1. trip_posts
    `CREATE TABLE [dbo].[trip_posts](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [user_id] [int] NOT NULL,
        [rental_id] [int] NULL,
        [title] [nvarchar](255) NULL,
        [content] [nvarchar](max) NULL,
        [location] [nvarchar](255) NULL,
        [image_url] [nvarchar](max) NULL,
        [created_at] [datetime] NULL DEFAULT GETDATE(),
    PRIMARY KEY CLUSTERED ([id] ASC)
    )`,

    `ALTER TABLE [dbo].[trip_posts] ADD CONSTRAINT [FK_posts_user] FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([id])`,
    `ALTER TABLE [dbo].[trip_posts] ADD CONSTRAINT [FK_posts_rental] FOREIGN KEY([rental_id]) REFERENCES [dbo].[rental] ([id])`,

    // 2. post_comments
    `CREATE TABLE [dbo].[post_comments](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [post_id] [int] NOT NULL,
        [user_id] [int] NOT NULL,
        [content] [nvarchar](max) NULL,
        [created_at] [datetime] NULL DEFAULT GETDATE(),
    PRIMARY KEY CLUSTERED ([id] ASC)
    )`,

    `ALTER TABLE [dbo].[post_comments] ADD CONSTRAINT [FK_comments_post] FOREIGN KEY([post_id]) REFERENCES [dbo].[trip_posts] ([id]) ON DELETE CASCADE`,
    `ALTER TABLE [dbo].[post_comments] ADD CONSTRAINT [FK_comments_user] FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([id])`,

    // 3. post_likes
    `CREATE TABLE [dbo].[post_likes](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [post_id] [int] NOT NULL,
        [user_id] [int] NOT NULL,
        [created_at] [datetime] NULL DEFAULT GETDATE(),
    PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT [UQ_post_user_like] UNIQUE ([post_id], [user_id])
    )`,

    `ALTER TABLE [dbo].[post_likes] ADD CONSTRAINT [FK_likes_post] FOREIGN KEY([post_id]) REFERENCES [dbo].[trip_posts] ([id]) ON DELETE CASCADE`,
    `ALTER TABLE [dbo].[post_likes] ADD CONSTRAINT [FK_likes_user] FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([id])`,

    // 4. reviews
    `CREATE TABLE [dbo].[reviews](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [rental_id] [int] NOT NULL,
        [user_id] [int] NOT NULL,
        [rating] [int] NOT NULL,
        [comment] [nvarchar](max) NULL,
        [created_at] [datetime] NULL DEFAULT GETDATE(),
    PRIMARY KEY CLUSTERED ([id] ASC),
    CONSTRAINT [UQ_rental_review] UNIQUE ([rental_id])
    )`,

    `ALTER TABLE [dbo].[reviews] ADD CONSTRAINT [FK_reviews_rental] FOREIGN KEY([rental_id]) REFERENCES [dbo].[rental] ([id])`,
    `ALTER TABLE [dbo].[reviews] ADD CONSTRAINT [FK_reviews_user] FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([id])`,

    // 5. trip_images
    `CREATE TABLE [dbo].[trip_images](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [post_id] [int] NOT NULL,
        [image_url] [nvarchar](500) NULL,
        [created_at] [datetime] NULL DEFAULT GETDATE(),
    PRIMARY KEY CLUSTERED ([id] ASC)
    )`,

    `ALTER TABLE [dbo].[trip_images] ADD CONSTRAINT [FK_images_post] FOREIGN KEY([post_id]) REFERENCES [dbo].[trip_posts] ([id]) ON DELETE CASCADE`
];

sql.connect(dbConfig).then(async pool => {
    console.log("Creating community and review tables...");
    for (const statement of tablesSql) {
        try {
            await pool.request().query(statement);
            console.log("Executed successfully:", statement.substring(0, 45) + "...");
        } catch (e) {
            if (e.message.includes("already an object named") || e.message.includes("already exists")) {
                console.log("Object already exists, skipping statement.");
            } else {
                throw e;
            }
        }
    }
    console.log("All tables created successfully.");
    process.exit(0);
}).catch(err => {
    console.error("Error creating tables:", err);
    process.exit(1);
});
