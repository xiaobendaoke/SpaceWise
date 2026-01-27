package com.example.myapplication.templates

data class SpaceTemplate(
    val id: String,
    val name: String,
    val spotNames: List<String>,
    val defaultTags: List<TemplateTag>,
)

data class TemplateTag(
    val name: String,
    val parentName: String? = null,
)

object Templates {
    val all: List<SpaceTemplate> = listOf(
        SpaceTemplate(
            id = "closet",
            name = "衣柜",
            spotNames = listOf("上层", "中层", "下层", "抽屉", "配饰盒"),
            defaultTags = listOf(
                TemplateTag("上衣"),
                TemplateTag("裤子"),
                TemplateTag("外套"),
                TemplateTag("内衣"),
                TemplateTag("袜子"),
                TemplateTag("配饰", parentName = null),
            )
        ),
        SpaceTemplate(
            id = "medicine",
            name = "药箱",
            spotNames = listOf("常用药", "外用药", "器材", "儿童", "备品"),
            defaultTags = listOf(
                TemplateTag("退烧"),
                TemplateTag("感冒"),
                TemplateTag("肠胃"),
                TemplateTag("外伤"),
                TemplateTag("儿童"),
            )
        ),
        SpaceTemplate(
            id = "tools",
            name = "工具箱",
            spotNames = listOf("螺丝刀", "扳手", "电工", "耗材", "杂项"),
            defaultTags = listOf(
                TemplateTag("五金"),
                TemplateTag("电工"),
                TemplateTag("耗材"),
            )
        ),
        SpaceTemplate(
            id = "baby",
            name = "母婴",
            spotNames = listOf("尿不湿", "湿巾", "奶瓶", "衣物", "护理"),
            defaultTags = listOf(
                TemplateTag("消耗品"),
                TemplateTag("衣物"),
                TemplateTag("护理"),
            )
        ),
    )
}

