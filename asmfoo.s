.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
sub $16, %rsp
mov -8(%rsp), %r11
mov $30, %r11
mov %r11, -8(%rsp)
mov -16(%rsp), %r11
mov $40, %r11
mov %r11, -16(%rsp)
mov -16(%rsp), %r10
mov -8(%rsp), %r11
add %r10, %r11
mov %r10, -16(%rsp)
mov %r11, -8(%rsp)
mov -8(%rsp), %r10
mov %r10, %rax
mov %r10, -8(%rsp)
add $16, %rsp
ret
