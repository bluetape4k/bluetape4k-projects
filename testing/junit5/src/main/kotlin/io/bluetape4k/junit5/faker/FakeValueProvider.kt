package io.bluetape4k.junit5.faker

object FakeValueProvider {
    private const val ADDRESS = "address"

    object Address {
        const val StreetName = "$ADDRESS.streetName"
        const val StreetAddressNumber = "$ADDRESS.streetAddressNumber"
        const val StreetAddress = "$ADDRESS.streetAddress"
        const val SecondaryAddress = "$ADDRESS.secondaryAddress"
        const val ZipCode = "$ADDRESS.zipCode"
    }

    private const val AVIATION = "aviation"

    object Aviation {
        const val Aircraft = "$AVIATION.aircraft"
        const val Airport = "$AVIATION.airport"
        const val Metar = "$AVIATION.METAR"
    }


    private const val ANCIENT = "ancient"

    object Ancient {
        const val God = "$ANCIENT.god"
        const val Primordial = "$ANCIENT.primordial"
        const val Titan = "$ANCIENT.titan"
        const val Hero = "$ANCIENT.hero"
    }

    private const val AWS = "aws"

    object Aws {
        const val AmiId = "$AWS.amiId"
        const val InstanceId = "$AWS.instanceId"
        const val InstanceType = "$AWS.instanceType"
        const val AvailabilityZone = "$AWS.availabilityZone"
        const val Region = "$AWS.region"
        const val AccountId = "$AWS.accountId"
        const val CanonicalId = "$AWS.canonicalId"
        const val EmailAddress = "$AWS.emailAddress"
        const val ImageId = "$AWS.imageId"
        const val InstanceIdentityDocument = "$AWS.instanceIdentityDocument"
        const val IpV4 = "$AWS.ipv4"
        const val IpV6 = "$AWS.ipv6"
        const val Prefix = "$AWS.prefix"
        const val RamArn = "$AWS.ramArn"
        const val RamResourceShareArn = "$AWS.ramResourceShareArn"
        const val S3Bucket = "$AWS.s3Bucket"
        const val S3Object = "$AWS.s3Object"
        const val S3ObjectVersion = "$AWS.s3ObjectVersion"
        const val S3Region = "$AWS.s3Region"
        const val SecurityGroupId = "$AWS.securityGroupId"
        const val SecurityGroupName = "$AWS.securityGroupName"
        const val SecurityGroupVpcId = "$AWS.securityGroupVpcId"
        const val SubnetId = "$AWS.subnetId"
        const val VolumeId = "$AWS.volumeId"
        const val VpcId = "$AWS.vpcId"
    }

    private const val AZURE = "azure"

    object Azure {
        const val Location = "$AZURE.location"
        const val ResourceGroup = "$AZURE.resourceGroup"
        const val ResourceName = "$AZURE.resourceName"
        const val ResourceId = "$AZURE.resourceId"
        const val SubscriptionId = "$AZURE.subscriptionId"
        const val VirtualMachineId = "$AZURE.virtualMachineId"
        const val VirtualMachineName = "$AZURE.virtualMachineName"
        const val VirtualMachineScaleSetId = "$AZURE.virtualMachineScaleSetId"
        const val VirtualMachineScaleSetName = "$AZURE.virtualMachineScaleSetName"
        const val VirtualNetworkId = "$AZURE.virtualNetworkId"
        const val VirtualNetworkName = "$AZURE.virtualNetworkName"
        const val IpAddress = "$AZURE.ipAddress"
        const val IpAddressPrivate = "$AZURE.ipAddressPrivate"
        const val IpAddressPublic = "$AZURE.ipAddressPublic"
        const val IpAddressPublicIpv4 = "$AZURE.ipAddressPublicIpv4"
        const val IpAddressPublicIpv6 = "$AZURE.ipAddressPublicIpv6"
    }

    private const val BARCODE = "barcode"

    object Barcode {
        const val Ean8 = "$BARCODE.ean8"
        const val Ean13 = "$BARCODE.ean13"
        const val UpcA = "$BARCODE.upcA"
        const val UpcE = "$BARCODE.upcE"
        const val Gtin8 = "$BARCODE.gtin8"
        const val Gtin13 = "$BARCODE.gtin13"
        const val Gtin14 = "$BARCODE.gtin14"
        const val Sscc = "$BARCODE.sscc"
        const val Isbn10 = "$BARCODE.isbn10"
        const val Isbn13 = "$BARCODE.isbn13"
    }

    private const val NAME = "name"

    object Name {
        const val FullName = "$NAME.fullName"
        const val FirstName = "$NAME.firstName"
        const val LastName = "$NAME.lastName"
        const val Username = "$NAME.username"
        const val Title = "$NAME.title"
    }

    private const val APP = "app"

    object App {
        const val Name = "$APP.name"
        const val Version = "$APP.version"
        const val Author = "$APP.author"
    }

    private const val ARTIST = "artist"

    object Artist {
        const val Name = "$ARTIST.name"
    }

    private const val BOOK = "book"

    object Book {
        const val Title = "$BOOK.title"
        const val Author = "$BOOK.author"
        const val Publisher = "$BOOK.publisher"
        const val Genre = "$BOOK.genre"
    }

    private const val BUSINESS = "business"

    object Business {
        const val CreditCardNumber = "$BUSINESS.creditCardNumber"
        const val CreditCardExpiry = "$BUSINESS.creditCardExpiry"
        const val CreditCardType = "$BUSINESS.creditCardType"
        const val CreditCardCvv = "$BUSINESS.creditCardCvv"
        const val CreditCard = "$BUSINESS.creditCard"
        const val CreditCardExpiringSoon = "$BUSINESS.creditCardExpiringSoon"
        const val CreditCardExpiringBetween = "$BUSINESS.creditCardExpiringBetween"
        const val CreditCardExpiringIn = "$BUSINESS.creditCardExpiringIn"
        const val CreditCardExpiringThisMonth = "$BUSINESS.creditCardExpiringThisMonth"
        const val CreditCardExpiringNextMonth = "$BUSINESS.creditCardExpiringNextMonth"
        const val CreditCardExpiringInMonth = "$BUSINESS.creditCardExpiringInMonth"
        const val CreditCardExpiringInMonths = "$BUSINESS.creditCardExpiringInMonths"
        const val CreditCardExpiringInYear = "$BUSINESS.creditCardExpiringInYear"
        const val CreditCardExpiringInYears = "$BUSINESS.creditCardExpiringInYears"
        const val CreditCardExpiringOn = "$BUSINESS.creditCardExpiringOn"
    }

    private const val CODE = "code"

    object Code {
        const val Asin = "$CODE.asin"
        const val Ean8 = "$CODE.ean8"
        const val Ean13 = "$CODE.ean13"
        const val Imei = "$CODE.imei"
        const val Isbn10 = "$CODE.isbn10"
        const val Isbn13 = "$CODE.isbn13"
        const val IsbnGroup = "$CODE.isbnGroup"
        const val IsbnGs1 = "$CODE.isbnGs1"
    }

    private const val COLOR = "color"

    object Color {
        const val Name = "$COLOR.name"
        const val Hex = "$COLOR.hex"
        const val SafeName = "$COLOR.safeName"
        const val SafeHex = "$COLOR.safeHex"
    }

    private const val COMMERCE = "commerce"

    object Commerce {
        const val Department = "$COMMERCE.department"
        const val ProductName = "$COMMERCE.productName"
        const val Price = "$COMMERCE.price"
        const val PromotionCode = "$COMMERCE.promotionCode"
    }

    private const val COMPANY = "company"

    object Company {
        const val Name = "$COMPANY.name"
        const val Industry = "$COMPANY.industry"
        const val Profession = "$COMPANY.profession"
        const val Suffix = "$COMPANY.suffix"
        const val CatchPhrase = "$COMPANY.catchPhrase"
        const val Bs = "$COMPANY.bs"
        const val Ein = "$COMPANY.ein"
    }

    private const val COMPUTER = "computer"

    object Computer {
        const val MacAddress = "$COMPUTER.macAddress"
        const val IpV4Address = "$COMPUTER.ipv4Address"
        const val IpV6Address = "$COMPUTER.ipv6Address"
        const val Url = "$COMPUTER.url"
        const val DomainName = "$COMPUTER.domainName"
        const val DomainSuffix = "$COMPUTER.domainSuffix"
        const val UserName = "$COMPUTER.userName"
        const val Password = "$COMPUTER.password"
        const val Md5 = "$COMPUTER.md5"
        const val Sha1 = "$COMPUTER.sha1"
        const val Sha256 = "$COMPUTER.sha256"
        const val Locale = "$COMPUTER.locale"
        const val TimeZone = "$COMPUTER.timeZone"
    }

    private const val COUNTRY = "country"

    object Country {
        const val Name = "$COUNTRY.name"
        const val Code = "$COUNTRY.code"
        const val CapitalCity = "$COUNTRY.capitalCity"
        const val Continent = "$COUNTRY.continent"
        const val Currency = "$COUNTRY.currency"
        const val Flag = "$COUNTRY.flag"
        const val Language = "$COUNTRY.language"
        const val Latitude = "$COUNTRY.latitude"
        const val Longitude = "$COUNTRY.longitude"
        const val Emoji = "$COUNTRY.emoji"
        const val EmojiCode = "$COUNTRY.emojiCode"
        const val EmojiU = "$COUNTRY.emojiU"
    }

    private const val CURRENCY = "currency"

    object Currency {
        const val Name = "$CURRENCY.name"
        const val Code = "$CURRENCY.code"
        const val Symbol = "$CURRENCY.symbol"
    }

    private const val DATE = "date"

    object Date {
        const val Birthday = "$DATE.birthday"
        const val Between = "$DATE.between"
        const val BetweenExcept = "$DATE.betweenExcept"
        const val BetweenExceptInstant = "$DATE.betweenExceptInstant"
        const val BetweenInstant = "$DATE.betweenInstant"
        const val BirthdayExcept = "$DATE.birthdayExcept"
        const val BirthdayExceptInstant = "$DATE.birthdayExceptInstant"
        const val BirthdayInstant = "$DATE.birthdayInstant"
        const val Future = "$DATE.future"
        const val FutureInstant = "$DATE.futureInstant"
        const val Past = "$DATE.past"
        const val PastInstant = "$DATE.pastInstant"
        const val Recent = "$DATE.recent"
        const val RecentInstant = "$DATE.recentInstant"
        const val Soon = "$DATE.soon"
        const val SoonInstant = "$DATE.soonInstant"
        const val SoonOffset = "$DATE.soonOffset"
        const val SoonOffsetDateTime = "$DATE.soonOffsetDateTime"
        const val SoonZonedDateTime = "$DATE.soonZonedDateTime"
        const val SoonLocalDateTime = "$DATE.soonLocalDateTime"
        const val SoonLocalDate = "$DATE.soonLocalDate"
        const val SoonLocalTime = "$DATE.soonLocalTime"
        const val SoonInstantOffset = "$DATE.soonInstantOffset"
        const val SoonInstantOffsetDateTime = "$DATE.soonInstantOffsetDateTime"
        const val SoonInstantZonedDateTime = "$DATE.soonInstantZonedDateTime"
        const val SoonInstantLocalDateTime = "$DATE.soonInstantLocalDateTime"
        const val SoonInstantLocalDate = "$DATE.soonInstantLocalDate"
        const val SoonInstantLocalTime = "$DATE.soonInstantLocalTime"
        const val SoonOffsetOffset = "$DATE.soonOffsetOffset"
        const val SoonOffsetOffsetDateTime = "$DATE.soonOffsetOffsetDateTime"
        const val SoonOffsetZonedDateTime = "$DATE.soonOffsetZonedDateTime"
        const val SoonOffsetLocalDateTime = "$DATE.soonOffsetLocalDateTime"
        const val SoonOffsetLocalDate = "$DATE.soonOffsetLocalDate"
        const val SoonOffsetLocalTime = "$DATE.soonOffsetLocalTime"
        const val SoonZonedOffset = "$DATE.soonZonedOffset"
        const val SoonZonedOffsetDateTime = "$DATE.soonZonedOffsetDateTime"
        const val SoonZonedZonedDateTime = "$DATE.soonZonedZonedDateTime"
    }


    private const val DRIVING_LICENSE = "drivingLicense"

    object DrivingLicense {
        const val DrivingLicense = "$DRIVING_LICENSE.drivingLicense"
    }

    private const val EDUCATOR = "educator"

    object Educator {
        const val University = "$EDUCATOR.university"
        const val Course = "$EDUCATOR.course"
    }

    private const val EMOJI = "emoji"

    object Emoji {
        const val Cat = "$EMOJI.cat"
        const val Smiley = "$EMOJI.smiley"
    }

    private const val FILE = "file"

    object File {
        const val Extension = "$FILE.extension"
        const val FileName = "$FILE.fileName"
        const val MimeType = "$FILE.mimeType"
    }

    private const val FINANCE = "finance"

    object Finance {
        const val NasdaqTicker = "$FINANCE.nasdaqTicker"
        const val NyseSymbol = "$FINANCE.nyseSymbol"
        const val StockMarket = "$FINANCE.stockMarket"
        const val CreditCard = "$FINANCE.creditCard"
        const val Bic = "$FINANCE.bic"
        const val Iban = "$FINANCE.iban"
        const val CreditCardType = "$FINANCE.creditCardType"
    }

    private const val FOOD = "food"

    object Food {
        const val Ingredient = "$FOOD.ingredient"
        const val Spices = "$FOOD.spices"
        const val Measurement = "$FOOD.measurement"
        const val MeasurementSize = "$FOOD.measurementSize"
        const val Dish = "$FOOD.dish"
        const val Fruits = "$FOOD.fruits"
        const val Vegetable = "$FOOD.vegetable"
    }

    private const val FRIENDS = "friends"

    object Friends {
        const val Character = "$FRIENDS.character"
        const val Location = "$FRIENDS.location"
        const val Quote = "$FRIENDS.quote"
    }
}
