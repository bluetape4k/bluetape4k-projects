package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.filter.ChromeFilter
import com.sksamuel.scrimage.filter.GothamFilter
import com.sksamuel.scrimage.filter.GrayscaleFilter
import com.sksamuel.scrimage.filter.InvertFilter
import com.sksamuel.scrimage.filter.NashvilleFilter
import com.sksamuel.scrimage.filter.OldPhotoFilter
import com.sksamuel.scrimage.filter.SepiaFilter
import com.sksamuel.scrimage.filter.SummerFilter
import com.sksamuel.scrimage.filter.VintageFilter

/** 세피아 톤 효과를 적용합니다. */
fun ImageFilterChain.sepia() {
    addNative(SepiaFilter())
}

/** 이미지를 흑백으로 변환합니다. */
fun ImageFilterChain.grayscale() {
    addNative(GrayscaleFilter())
}

/** 색상을 반전합니다. */
fun ImageFilterChain.invert() {
    addNative(InvertFilter())
}

/** 빈티지 필름 효과를 적용합니다. */
fun ImageFilterChain.vintage() {
    addNative(VintageFilter())
}

/** 크롬 효과를 적용합니다. */
fun ImageFilterChain.chrome() {
    addNative(ChromeFilter())
}

/** Nashville 인스타그램 필터 효과를 적용합니다. */
fun ImageFilterChain.nashville() {
    addNative(NashvilleFilter())
}

/** Gotham 인스타그램 필터 효과를 적용합니다. */
fun ImageFilterChain.gotham() {
    addNative(GothamFilter())
}

/** Summer 인스타그램 필터 효과를 적용합니다. */
fun ImageFilterChain.summer() {
    addNative(SummerFilter(true))
}

/** 오래된 사진 효과를 적용합니다. */
fun ImageFilterChain.oldPhoto() {
    addNative(OldPhotoFilter())
}
